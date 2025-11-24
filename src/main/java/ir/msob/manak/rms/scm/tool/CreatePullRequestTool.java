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

import static ir.msob.manak.domain.model.rms.RmsConstants.*;

@Service
@RequiredArgsConstructor
public class CreatePullRequestTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(CreatePullRequestTool.class);

    private final ScmOperationService scmOperationService;

    @Override
    public ToolDescriptor getToolDescriptor() {
        // ==== Parameters ====
        ParameterDescriptor repositoryIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Repository ID where the pull request will be created")
                .required(true)
                .example("repo-001")
                .nullable(false)
                .build();

        ParameterDescriptor sourceBranchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Source branch of the pull request")
                .required(true)
                .example("feature/x")
                .nullable(false)
                .build();

        ParameterDescriptor targetBranchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Target branch for merging the pull request")
                .required(true)
                .example("main")
                .nullable(false)
                .build();

        ParameterDescriptor titleParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Title of the pull request")
                .required(true)
                .example("Add feature X")
                .nullable(false)
                .build();

        ParameterDescriptor descriptionParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Optional description of the pull request")
                .required(false)
                .example("This PR introduces ...")
                .nullable(true)
                .build();

        // ==== Response Schema ====
        ParameterDescriptor responseSchema = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("Details of the created pull request")
                .property("id", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Pull request ID")
                        .required(true)
                        .build())
                .property("title", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Pull request title")
                        .required(true)
                        .build())
                .property("sourceBranch", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Source branch")
                        .required(true)
                        .build())
                .property("targetBranch", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Target branch")
                        .required(true)
                        .build())
                .property("status", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Pull request status")
                        .required(true)
                        .build())
                .build();

        // ==== ToolDescriptor ====
        return ToolDescriptor.builder()
                .category("Repository")
                .name("CreatePullRequest")
                .displayName("Create Pull Request")
                .description("Creates a pull/merge request from source to target branch")
                .version("1.0.0")
                .tag("git")
                .tag("pull-request")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("sourceBranch", sourceBranchParam)
                .parameter("targetBranch", targetBranchParam)
                .parameter("title", titleParam)
                .parameter("description", descriptionParam)
                .response(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseDescriptor.builder()
                        .responseSchema(responseSchema)
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("SUCCESS")
                                .description("Pull request created successfully")
                                .contentType("application/json")
                                .build())
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("ERROR")
                                .description("An error occurred while creating the pull request")
                                .contentType("application/json")
                                .build())
                        .example(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.Example.builder()
                                .title("Create Pull Request Example")
                                .description("Creates a PR from feature/x to main branch")
                                .input(Map.of(
                                        "repositoryId", "repo-001",
                                        "sourceBranch", "feature/x",
                                        "targetBranch", "main",
                                        "title", "Add feature X",
                                        "description", "This PR introduces ..."))
                                .output(Map.of(
                                        "id", "42",
                                        "title", "Add feature X",
                                        "sourceBranch", "feature/x",
                                        "targetBranch", "main",
                                        "status", "OPEN"))
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
        String src = VariableUtils.safeString(request.getParameters().get(SOURCE_BRANCH_KEY));
        String tgt = VariableUtils.safeString(request.getParameters().get(TARGET_BRANCH_KEY));
        String title = VariableUtils.safeString(request.getParameters().get(TITLE_KEY));
        String description = VariableUtils.safeString(request.getParameters().get(DESCRIPTION_KEY));

        log.info("[{}] Creating PR: repo={}, {} -> {}", toolId, repositoryId, src, tgt);

        return scmOperationService.createPullRequest(repositoryId, src, tgt, title, description, user)
                .map(pr -> InvokeResponse.builder()
                        .requestId(requestId)
                        .toolId(toolId)
                        .result(pr)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error creating PR", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .requestId(requestId)
                            .toolId(toolId)
                            .error(InvokeResponse.ErrorInfo.builder()
                                    .code("CREATE_PR_ERROR")
                                    .message(ToolExecutorUtil.buildErrorResponse(request.getToolId(), e))
                                    .stackTrace(Arrays.toString(e.getStackTrace()))
                                    .detail("repositoryId", repositoryId)
                                    .detail("sourceBranch", src)
                                    .detail("targetBranch", tgt)
                                    .build())
                            .executedAt(Instant.now())
                            .build());
                });
    }
}