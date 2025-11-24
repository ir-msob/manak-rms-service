package ir.msob.manak.rms.scm.tool;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.common.model.ParameterDescriptor;
import ir.msob.manak.domain.model.toolhub.ToolExecutor;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ToolDescriptor;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.service.toolhub.util.ToolExecutorUtil;
import ir.msob.manak.rms.scm.scmprovider.ScmOperationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static ir.msob.manak.domain.model.rms.RmsConstants.PULL_REQUEST_ID_KEY;
import static ir.msob.manak.domain.model.rms.RmsConstants.REPOSITORY_ID_KEY;

@Service
@RequiredArgsConstructor
public class ClosePullRequestTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ClosePullRequestTool.class);

    private final ScmOperationService scmOperationService;

    @Override
    public ToolDescriptor getToolDescriptor() {
        // ==== Parameters ====
        ParameterDescriptor repositoryIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Repository ID where the pull request exists")
                .required(true)
                .example("repo-001")
                .nullable(false)
                .build();

        ParameterDescriptor prIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("ID of the pull request to close")
                .required(true)
                .example("42")
                .nullable(false)
                .build();

        // ==== Response Schema ====
        ParameterDescriptor responseSchema = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("Result of closing the pull request")
                .property("message", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Informational message about the operation")
                        .required(true)
                        .build())
                .build();

        // ==== ToolDescriptor ====
        return ToolDescriptor.builder()
                .category("Repository")
                .name("ClosePullRequest")
                .displayName("Close Pull Request")
                .description("Closes a pull request without merging the changes")
                .version("1.0.0")
                .tag("git")
                .tag("pull-request")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("pullRequestId", prIdParam)
                .response(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseDescriptor.builder()
                        .responseSchema(responseSchema)
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("SUCCESS")
                                .description("Pull request closed successfully")
                                .contentType("application/json")
                                .build())
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("ERROR")
                                .description("An error occurred while closing the pull request")
                                .contentType("application/json")
                                .build())
                        .example(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.Example.builder()
                                .title("Close pull request example")
                                .description("Demonstrates closing a pull request in a repository")
                                .input(Map.of(
                                        "repositoryId", "repo-001",
                                        "pullRequestId", "42"))
                                .output(Map.of(
                                        "message", "Pull request 42 has been successfully closed"))
                                .build())
                        .build())
                .retryPolicy(ir.msob.manak.domain.model.common.model.RetryPolicy.builder()
                        .enabled(true)
                        .maxAttempts(3)
                        .initialIntervalMs(500)
                        .multiplier(2.0)
                        .maxIntervalMs(2000)
                        .build())
                .timeoutPolicy(ir.msob.manak.domain.model.common.model.TimeoutPolicy.builder()
                        .timeoutMs(5000)
                        .failFast(false)
                        .gracePeriodMs(1000)
                        .build())
                .status(ToolDescriptor.ToolDescriptorStatus.ACTIVE)
                .build();
    }


    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String requestId = request.getRequestId();
        String toolId = request.getToolId();
        String repositoryId = VariableUtils.safeString(request.getParameters().get(REPOSITORY_ID_KEY));
        String prId = VariableUtils.safeString(request.getParameters().get(PULL_REQUEST_ID_KEY));

        log.info("[{}] Closing PR: repo={}, prId={}", toolId, repositoryId, prId);

        return scmOperationService.closePullRequest(repositoryId, prId, user)
                .map(res -> InvokeResponse.builder()
                        .requestId(requestId)
                        .toolId(toolId)
                        .result(res)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error closing PR", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .requestId(requestId)
                            .toolId(toolId)
                            .error(InvokeResponse.ErrorInfo.builder()
                                    .code("CLOSE_PR_ERROR")
                                    .message(ToolExecutorUtil.buildErrorResponse(request.getToolId(), e))
                                    .stackTrace(Arrays.toString(e.getStackTrace()))
                                    .detail("repositoryId", repositoryId)
                                    .detail("pullRequestId", prId)
                                    .build())
                            .executedAt(Instant.now())
                            .build());
                });
    }
}