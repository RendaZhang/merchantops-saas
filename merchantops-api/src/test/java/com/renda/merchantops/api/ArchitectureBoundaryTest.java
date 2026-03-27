package com.renda.merchantops.api;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.renda.merchantops")
class ArchitectureBoundaryTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring_or_jpa =
            noClasses().that().resideInAnyPackage("com.renda.merchantops.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule activated_api_boundary_should_not_depend_on_infra =
            noClasses().that().haveFullyQualifiedName("com.renda.merchantops.api.auth.AuthService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.user.UserQueryService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.user.UserCommandService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.rbac.RoleQueryService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.ticket.TicketQueryService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.ticket.TicketCommandService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.ticket.ai.TicketAiSummaryService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.ticket.ai.TicketAiTriageService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.ticket.ai.TicketAiReplyDraftService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.audit.AuditEventService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.approval.ApprovalRequestCommandService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.approval.ApprovalRequestQueryService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.importjob.ImportJobSubmissionService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.importjob.ImportJobReplayService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.importjob.ImportJobQueryService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.importjob.ai.ImportJobAiErrorSummaryService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.importjob.messaging.ImportJobExecutionCoordinator")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.importjob.messaging.ImportJobQueueRecoveryService")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.importjob.messaging.UserCsvImportProcessor")
                    .or().haveFullyQualifiedName("com.renda.merchantops.api.security.CurrentUserAccessValidator")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.renda.merchantops.infra..");

    @ArchTest
    static final ArchRule capability_code_should_not_live_in_root_horizontal_packages =
            noClasses().that().resideInAnyPackage(
                            "com.renda.merchantops.api.controller..",
                            "com.renda.merchantops.api.contract..",
                            "com.renda.merchantops.api.service..",
                            "com.renda.merchantops.api.messaging.."
                    )
                    .should().haveSimpleNameStartingWith("Ticket")
                    .orShould().haveSimpleNameStartingWith("ImportJob")
                    .orShould().haveSimpleNameStartingWith("ApprovalRequest")
                    .orShould().haveSimpleNameStartingWith("AuditEvent");

    @ArchTest
    static final ArchRule api_response_should_live_in_platform_response =
            classes().that().haveSimpleName("ApiResponse")
                    .should().resideInAnyPackage("com.renda.merchantops.api.platform.response");
}
