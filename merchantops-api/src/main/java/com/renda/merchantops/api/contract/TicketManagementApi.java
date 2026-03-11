package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_TICKET_ASSIGN;
import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_TICKET_COMMENT_CREATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_TICKET_CREATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_TICKET_STATUS_UPDATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_TICKET_ASSIGNEE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_TICKET_LIST_FILTERS;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_TICKET_STATUS_TRANSITION;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_TICKET_ASSIGNED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_TICKET_COMMENT_CREATED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_TICKET_CREATED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_TICKET_DETAIL;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_TICKET_LIST;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_TICKET_STATUS_UPDATED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_TICKET_ASSIGN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_TICKET_COMMENT_CREATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_TICKET_CREATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_TICKET_STATUS;

@Tag(name = "Ticket Workflow")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/tickets")
public interface TicketManagementApi {

    @Operation(
            summary = "Page tickets in current tenant",
            description = "Requires TICKET_READ permission. Supports page/size plus optional status, assigneeId, keyword (title/description), and unassignedOnly filters. The tenant scope is derived from JWT, not request parameters. assigneeId cannot be combined with unassignedOnly=true."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Query successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_TICKET_LIST))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing TICKET_READ permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid query filter combination",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_BAD_REQUEST_TICKET_LIST_FILTERS))
            )
    })
    @GetMapping
    ApiResponse<TicketPageResponse> listTickets(@ParameterObject TicketPageQuery query);

    @Operation(
            summary = "Get ticket detail in current tenant",
            description = "Requires TICKET_READ permission. Returns ticket core fields plus comments and workflow-level operation logs."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Query successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_TICKET_DETAIL))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing TICKET_READ permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found in current tenant",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":\"NOT_FOUND\",\"message\":\"ticket not found\",\"data\":null}"))
            )
    })
    @GetMapping("/{id}")
    ApiResponse<TicketDetailResponse> getTicketDetail(@PathVariable("id") Long id);

    @Operation(
            summary = "Create ticket in current tenant",
            description = "Requires TICKET_WRITE permission. New tickets start in OPEN status, capture the requestId from the current request, and write a CREATED workflow log entry."
    )
    @RequestBody(
            required = true,
            description = "Ticket create payload",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_TICKET_CREATE))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Create successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_TICKET_CREATED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_VALIDATION_ERROR_TICKET_CREATE))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing TICKET_WRITE permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            )
    })
    @PostMapping
    ApiResponse<TicketWriteResponse> createTicket(@Valid @org.springframework.web.bind.annotation.RequestBody TicketCreateRequest request);

    @Operation(
            summary = "Assign ticket to a tenant user",
            description = "Requires TICKET_WRITE permission. The assignee must exist and be ACTIVE in the current tenant. This writes an ASSIGNED workflow log entry."
    )
    @RequestBody(
            required = true,
            description = "Ticket assignee payload",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_TICKET_ASSIGN))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Assignment successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_TICKET_ASSIGNED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or assignee is invalid",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "validationError", value = RESP_VALIDATION_ERROR_TICKET_ASSIGN),
                            @ExampleObject(name = "badAssignee", value = RESP_BAD_REQUEST_TICKET_ASSIGNEE)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing TICKET_WRITE permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found in current tenant",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":\"NOT_FOUND\",\"message\":\"ticket not found\",\"data\":null}"))
            )
    })
    @PatchMapping("/{id}/assignee")
    ApiResponse<TicketWriteResponse> assignTicket(@PathVariable("id") Long id,
                                                  @Valid @org.springframework.web.bind.annotation.RequestBody TicketAssigneeUpdateRequest request);

    @Operation(
            summary = "Transition ticket status in current tenant",
            description = "Requires TICKET_WRITE permission. Allowed statuses are OPEN, IN_PROGRESS, and CLOSED. Valid transitions are OPEN -> IN_PROGRESS, OPEN -> CLOSED, IN_PROGRESS -> CLOSED, and CLOSED -> OPEN (reopen). This writes a STATUS_CHANGED workflow log entry."
    )
    @RequestBody(
            required = true,
            description = "Ticket status payload",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_TICKET_STATUS_UPDATE))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Status update successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_TICKET_STATUS_UPDATED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or transition is not allowed",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "validationError", value = RESP_VALIDATION_ERROR_TICKET_STATUS),
                            @ExampleObject(name = "invalidTransition", value = RESP_BAD_REQUEST_TICKET_STATUS_TRANSITION)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing TICKET_WRITE permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found in current tenant",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":\"NOT_FOUND\",\"message\":\"ticket not found\",\"data\":null}"))
            )
    })
    @PatchMapping("/{id}/status")
    ApiResponse<TicketWriteResponse> updateTicketStatus(@PathVariable("id") Long id,
                                                        @Valid @org.springframework.web.bind.annotation.RequestBody TicketStatusUpdateRequest request);

    @Operation(
            summary = "Add ticket comment in current tenant",
            description = "Requires TICKET_WRITE permission. Comments are recorded under the current operator and also emit a COMMENTED workflow log entry."
    )
    @RequestBody(
            required = true,
            description = "Ticket comment payload",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_TICKET_COMMENT_CREATE))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Comment create successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_TICKET_COMMENT_CREATED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_VALIDATION_ERROR_TICKET_COMMENT_CREATE))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing TICKET_WRITE permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found in current tenant",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":\"NOT_FOUND\",\"message\":\"ticket not found\",\"data\":null}"))
            )
    })
    @PostMapping("/{id}/comments")
    ApiResponse<TicketCommentResponse> addComment(@PathVariable("id") Long id,
                                                  @Valid @org.springframework.web.bind.annotation.RequestBody TicketCommentCreateRequest request);
}
