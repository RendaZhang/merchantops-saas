package com.renda.merchantops.api.config;

import com.renda.merchantops.domain.ai.AiInteractionRecordPort;
import com.renda.merchantops.domain.ai.AiInteractionRecordUseCase;
import com.renda.merchantops.domain.approval.ApprovalActionPort;
import com.renda.merchantops.domain.approval.ApprovalRequestPort;
import com.renda.merchantops.domain.approval.ApprovalRequestUseCase;
import com.renda.merchantops.domain.approval.ApprovalTargetUserPort;
import com.renda.merchantops.domain.audit.AuditEventPort;
import com.renda.merchantops.domain.audit.AuditEventUseCase;
import com.renda.merchantops.domain.auth.AccessValidationUseCase;
import com.renda.merchantops.domain.auth.AuthAccessPort;
import com.renda.merchantops.domain.auth.AuthenticationUseCase;
import com.renda.merchantops.domain.importjob.ImportJobCommandPort;
import com.renda.merchantops.domain.importjob.ImportJobCommandUseCase;
import com.renda.merchantops.domain.importjob.ImportJobQueryPort;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.auth.PasswordHasher;
import com.renda.merchantops.domain.ticket.TicketAuditPort;
import com.renda.merchantops.domain.ticket.TicketCommandPort;
import com.renda.merchantops.domain.ticket.TicketCommandUseCase;
import com.renda.merchantops.domain.ticket.TicketQueryPort;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import com.renda.merchantops.domain.user.RoleCatalogPort;
import com.renda.merchantops.domain.user.RoleQueryUseCase;
import com.renda.merchantops.domain.user.UserAuditPort;
import com.renda.merchantops.domain.user.UserCommandPort;
import com.renda.merchantops.domain.user.UserCommandUseCase;
import com.renda.merchantops.domain.user.UserQueryPort;
import com.renda.merchantops.domain.user.UserQueryUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainUseCaseConfig {

    @Bean
    public AuditEventUseCase auditEventUseCase(AuditEventPort auditEventPort) {
        return new com.renda.merchantops.domain.audit.AuditEventDomainService(auditEventPort);
    }

    @Bean
    public AiInteractionRecordUseCase aiInteractionRecordUseCase(AiInteractionRecordPort aiInteractionRecordPort) {
        return new com.renda.merchantops.domain.ai.AiInteractionRecordDomainService(aiInteractionRecordPort);
    }

    @Bean
    public ApprovalRequestUseCase approvalRequestUseCase(ApprovalRequestPort approvalRequestPort,
                                                         ApprovalTargetUserPort approvalTargetUserPort,
                                                         ApprovalActionPort approvalActionPort) {
        return new com.renda.merchantops.domain.approval.ApprovalRequestDomainService(
                approvalRequestPort,
                approvalTargetUserPort,
                approvalActionPort
        );
    }

    @Bean
    public AuthenticationUseCase authenticationUseCase(AuthAccessPort authAccessPort, PasswordHasher passwordHasher) {
        return new com.renda.merchantops.domain.auth.AuthenticationService(authAccessPort, passwordHasher);
    }

    @Bean
    public AccessValidationUseCase accessValidationUseCase(AuthAccessPort authAccessPort) {
        return new com.renda.merchantops.domain.auth.AccessValidationService(authAccessPort);
    }

    @Bean
    public UserQueryUseCase userQueryUseCase(UserQueryPort userQueryPort) {
        return new com.renda.merchantops.domain.user.UserQueryService(userQueryPort);
    }

    @Bean
    public RoleQueryUseCase roleQueryUseCase(RoleCatalogPort roleCatalogPort) {
        return new com.renda.merchantops.domain.user.RoleQueryService(roleCatalogPort);
    }

    @Bean
    public UserCommandUseCase userCommandUseCase(UserQueryPort userQueryPort,
                                                 RoleCatalogPort roleCatalogPort,
                                                 UserCommandPort userCommandPort,
                                                 PasswordHasher passwordHasher,
                                                 UserAuditPort userAuditPort) {
        return new com.renda.merchantops.domain.user.UserCommandService(
                userQueryPort,
                roleCatalogPort,
                userCommandPort,
                passwordHasher,
                userAuditPort
        );
    }

    @Bean
    public TicketQueryUseCase ticketQueryUseCase(TicketQueryPort ticketQueryPort) {
        return new com.renda.merchantops.domain.ticket.TicketQueryService(ticketQueryPort);
    }

    @Bean
    public ImportJobQueryUseCase importJobQueryUseCase(ImportJobQueryPort importJobQueryPort) {
        return new com.renda.merchantops.domain.importjob.ImportJobQueryDomainService(importJobQueryPort);
    }

    @Bean
    public ImportJobCommandUseCase importJobCommandUseCase(ImportJobCommandPort importJobCommandPort) {
        return new com.renda.merchantops.domain.importjob.ImportJobCommandDomainService(importJobCommandPort);
    }

    @Bean
    public TicketCommandUseCase ticketCommandUseCase(TicketCommandPort ticketCommandPort,
                                                     TicketAuditPort ticketAuditPort) {
        return new com.renda.merchantops.domain.ticket.TicketCommandService(ticketCommandPort, ticketAuditPort);
    }
}
