package ir.msob.manak.rms.scm.tool;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.common.model.ParameterDescriptor;
import ir.msob.manak.domain.model.toolhub.ToolExecutor;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ToolDescriptor;
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


@Service
@RequiredArgsConstructor
public class MergePullRequestTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(MergePullRequestTool.class);

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
                .description("Pull Request ID to merge")
                .required(true)
                .example("42")
                .nullable(false)
                .build();

        // ==== Response Schema ====
        ParameterDescriptor responseSchema = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("Result of merging the pull request")
                .property("merged", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.BOOLEAN)
                        .description("Indicates whether the PR was merged successfully")
                        .required(true)
                        .build())
                .property("pullRequestId", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Merged pull request ID")
                        .required(true)
                        .build())
                .build();

        // ==== ToolDescriptor ====
        return ToolDescriptor.builder()
                .category("Repository")
                .name("MergePullRequest")
                .displayName("Merge Pull Request")
                .description("Merges a pull request in the repository by ID")
                .version("1.0.0")
                .tag("git")
                .tag("pull-request")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("pullRequestId", prIdParam)
                .response(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseDescriptor.builder()
                        .responseSchema(responseSchema)
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("SUCCESS")
                                .description("Pull request merged successfully")
                                .contentType("application/json")
                                .build())
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("ERROR")
                                .description("An error occurred while merging the pull request")
                                .contentType("application/json")
                                .build())
                        .example(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.Example.builder()
                                .title("Merge Pull Request Example")
                                .description("Merges PR #42 in repo-001")
                                .input(Map.of(
                                        "repositoryId", "repo-001",
                                        "pullRequestId", "42"))
                                .output(Map.of(
                                        "merged", true,
                                        "pullRequestId", "42"))
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
        String repositoryId = (String) request.getParameters().get("repositoryId");
        String prId = (String) request.getParameters().get("pullRequestId");

        log.info("[{}] Merging PR: repo={}, prId={}", toolId, repositoryId, prId);

        return scmOperationService.mergePullRequest(repositoryId, prId, user)
                .map(res -> InvokeResponse.builder()
                        .requestId(requestId)
                        .toolId(toolId)
                        .result(res)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error merging PR", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .requestId(requestId)
                            .toolId(toolId)
                            .error(InvokeResponse.ErrorInfo.builder()
                                    .code("MERGE_PR_ERROR")
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