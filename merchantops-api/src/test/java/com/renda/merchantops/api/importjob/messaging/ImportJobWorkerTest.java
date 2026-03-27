package com.renda.merchantops.api.importjob.messaging;

import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.api.importjob.ImportFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobWorkerTest {

    @Mock
    private ImportFileStorageService importFileStorageService;
    @Mock
    private ImportJobExecutionCoordinator importJobExecutionCoordinator;

    private ImportProcessingProperties importProcessingProperties;
    private ImportJobWorker importJobWorker;

    @BeforeEach
    void setUp() {
        importProcessingProperties = new ImportProcessingProperties();
        importProcessingProperties.setChunkSize(2);
        importProcessingProperties.setMaxRowsPerJob(100);
        importJobWorker = new ImportJobWorker(importFileStorageService, importJobExecutionCoordinator, importProcessingProperties);
    }

    @Test
    void consumeShouldSplitRowsIntoConfiguredChunksAndCompleteJob() throws Exception {
        ImportJobExecutionContext context = new ImportJobExecutionContext(7001L, 1L, "USER_CSV", "1/key.csv", 101L, "req-1");
        when(importJobExecutionCoordinator.startProcessing(7001L, 1L))
                .thenReturn(ImportJobStartResult.started(context));
        when(importFileStorageService.openStream("1/key.csv")).thenReturn(new ByteArrayInputStream(
                ("""
                        username,displayName,email,password,roleCodes
                        alpha,Alpha User,alpha@example.com,123456,READ_ONLY
                        beta,Beta User,beta@example.com,123456,READ_ONLY
                        gamma,Gamma User,gamma@example.com,123456,READ_ONLY
                        delta,Delta User,delta@example.com,123456,READ_ONLY
                        epsilon,Epsilon User,epsilon@example.com,123456,READ_ONLY
                        """).getBytes(StandardCharsets.UTF_8)
        ));

        importJobWorker.consume(new ImportJobMessage(7001L, 1L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportJobChunkRow>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(importJobExecutionCoordinator, times(3)).processChunk(eq(context), chunkCaptor.capture());
        assertThat(chunkCaptor.getAllValues()).hasSize(3);
        assertThat(chunkCaptor.getAllValues().get(0)).extracting(ImportJobChunkRow::rowNumber)
                .containsExactly(2, 3);
        assertThat(chunkCaptor.getAllValues().get(1)).extracting(ImportJobChunkRow::rowNumber)
                .containsExactly(4, 5);
        assertThat(chunkCaptor.getAllValues().get(2)).extracting(ImportJobChunkRow::rowNumber)
                .containsExactly(6);
        verify(importJobExecutionCoordinator).completeJob(context);
        verify(importJobExecutionCoordinator, never()).failJob(any(), any());
    }

    @Test
    void consumeShouldFailUnsupportedImportTypeBeforeChunkProcessing() throws Exception {
        ImportJobExecutionContext context = new ImportJobExecutionContext(7002L, 1L, "TICKET_CSV", "1/key.csv", 101L, "req-2");
        when(importJobExecutionCoordinator.startProcessing(7002L, 1L))
                .thenReturn(ImportJobStartResult.started(context));
        when(importFileStorageService.openStream("1/key.csv")).thenReturn(new ByteArrayInputStream(
                ("""
                        username,displayName,email,password,roleCodes
                        valid,Valid User,valid@example.com,123456,READ_ONLY
                        """).getBytes(StandardCharsets.UTF_8)
        ));

        importJobWorker.consume(new ImportJobMessage(7002L, 1L));

        verify(importJobExecutionCoordinator, never()).processChunk(any(), any());
        ArgumentCaptor<ImportJobFailure> failureCaptor = ArgumentCaptor.forClass(ImportJobFailure.class);
        verify(importJobExecutionCoordinator).failJob(eq(context), failureCaptor.capture());
        assertThat(failureCaptor.getValue().errorCode()).isEqualTo("UNSUPPORTED_IMPORT_TYPE");
        assertThat(failureCaptor.getValue().errorSummary()).isEqualTo("unsupported import type");
    }

    @Test
    void consumeShouldPreserveQuotedCommaEscapedQuoteAndEmbeddedNewlineAcrossChunks() throws Exception {
        importProcessingProperties.setChunkSize(1);
        ImportJobExecutionContext context = new ImportJobExecutionContext(7003L, 1L, "USER_CSV", "1/quoted.csv", 101L, "req-3");
        when(importJobExecutionCoordinator.startProcessing(7003L, 1L))
                .thenReturn(ImportJobStartResult.started(context));
        when(importFileStorageService.openStream("1/quoted.csv")).thenReturn(new ByteArrayInputStream(
                ("""
                        username,displayName,email,password,roleCodes
                        quoted,"Escaped ""Quote"", User",quoted@example.com,123456,READ_ONLY
                        multiline,"Line 1
                        Line 2",multiline@example.com,123456,READ_ONLY
                        """).getBytes(StandardCharsets.UTF_8)
        ));

        importJobWorker.consume(new ImportJobMessage(7003L, 1L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportJobChunkRow>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(importJobExecutionCoordinator, times(2)).processChunk(eq(context), chunkCaptor.capture());
        assertThat(chunkCaptor.getAllValues().get(0)).singleElement().satisfies(row -> {
            assertThat(row.rowNumber()).isEqualTo(2);
            assertThat(row.columns()).containsExactly(
                    "quoted",
                    "Escaped \"Quote\", User",
                    "quoted@example.com",
                    "123456",
                    "READ_ONLY"
            );
        });
        assertThat(chunkCaptor.getAllValues().get(1)).singleElement().satisfies(row -> {
            assertThat(row.rowNumber()).isEqualTo(3);
            assertThat(row.columns()).containsExactly(
                    "multiline",
                    "Line 1\nLine 2",
                    "multiline@example.com",
                    "123456",
                    "READ_ONLY"
            );
        });
    }

    @Test
    void consumeShouldAcceptUtf8BomInFirstHeaderColumn() throws Exception {
        ImportJobExecutionContext context = new ImportJobExecutionContext(7004L, 1L, "USER_CSV", "1/bom.csv", 101L, "req-4");
        when(importJobExecutionCoordinator.startProcessing(7004L, 1L))
                .thenReturn(ImportJobStartResult.started(context));
        when(importFileStorageService.openStream("1/bom.csv")).thenReturn(new ByteArrayInputStream(
                ("\uFEFFusername,displayName,email,password,roleCodes\n" +
                        "bomuser,Bom User,bom@example.com,123456,READ_ONLY").getBytes(StandardCharsets.UTF_8)
        ));

        importJobWorker.consume(new ImportJobMessage(7004L, 1L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportJobChunkRow>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(importJobExecutionCoordinator).processChunk(eq(context), chunkCaptor.capture());
        assertThat(chunkCaptor.getValue()).singleElement().satisfies(row -> {
            assertThat(row.rowNumber()).isEqualTo(2);
            assertThat(row.columns()).containsExactly(
                    "bomuser",
                    "Bom User",
                    "bom@example.com",
                    "123456",
                    "READ_ONLY"
            );
        });
        verify(importJobExecutionCoordinator).completeJob(context);
    }

    @Test
    void consumeShouldFlushPendingChunkBeforeFailingMaxRowsExceeded() throws Exception {
        importProcessingProperties.setMaxRowsPerJob(5);
        ImportJobExecutionContext context = new ImportJobExecutionContext(7005L, 1L, "USER_CSV", "1/limit.csv", 101L, "req-5");
        when(importJobExecutionCoordinator.startProcessing(7005L, 1L))
                .thenReturn(ImportJobStartResult.started(context));
        when(importFileStorageService.openStream("1/limit.csv")).thenReturn(new ByteArrayInputStream(
                ("""
                        username,displayName,email,password,roleCodes
                        u1,User One,u1@example.com,123456,READ_ONLY
                        u2,User Two,u2@example.com,123456,READ_ONLY
                        u3,User Three,u3@example.com,123456,READ_ONLY
                        u4,User Four,u4@example.com,123456,READ_ONLY
                        u5,User Five,u5@example.com,123456,READ_ONLY
                        u6,User Six,u6@example.com,123456,READ_ONLY
                        """).getBytes(StandardCharsets.UTF_8)
        ));

        importJobWorker.consume(new ImportJobMessage(7005L, 1L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportJobChunkRow>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(importJobExecutionCoordinator, times(3)).processChunk(eq(context), chunkCaptor.capture());
        assertThat(chunkCaptor.getAllValues().get(0)).extracting(ImportJobChunkRow::rowNumber)
                .containsExactly(2, 3);
        assertThat(chunkCaptor.getAllValues().get(1)).extracting(ImportJobChunkRow::rowNumber)
                .containsExactly(4, 5);
        assertThat(chunkCaptor.getAllValues().get(2)).extracting(ImportJobChunkRow::rowNumber)
                .containsExactly(6);

        ArgumentCaptor<ImportJobFailure> failureCaptor = ArgumentCaptor.forClass(ImportJobFailure.class);
        verify(importJobExecutionCoordinator).failJob(eq(context), failureCaptor.capture());
        assertThat(failureCaptor.getValue().errorCode()).isEqualTo("MAX_ROWS_EXCEEDED");
        assertThat(failureCaptor.getValue().errorSummary()).isEqualTo("import job exceeded max row limit");
        assertThat(failureCaptor.getValue().rawPayload()).isEqualTo("u6,User Six,u6@example.com,123456,READ_ONLY");
        verify(importJobExecutionCoordinator, never()).completeJob(any());
    }

    @Test
    void consumeShouldAcknowledgeFreshProcessingRedeliveryWithoutLocalExecution() {
        when(importJobExecutionCoordinator.startProcessing(7006L, 1L))
                .thenReturn(ImportJobStartResult.requeue());

        importJobWorker.consume(new ImportJobMessage(7006L, 1L));

        verify(importJobExecutionCoordinator, never()).processChunk(any(), any());
        verify(importJobExecutionCoordinator, never()).completeJob(any());
        verify(importJobExecutionCoordinator, never()).failJob(any(), any());
    }
}
