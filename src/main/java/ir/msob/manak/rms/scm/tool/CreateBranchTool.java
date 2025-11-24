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
public class CreateBranchTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(CreateBranchTool.class);

    private final ScmOperationService scmOperationService;

    @Override
    public ToolDescriptor getToolDescriptor() {
        // ==== Parameters ====
        ParameterDescriptor repositoryIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Repository ID where the new branch will be created")
                .required(true)
                .example("repo-001")
                .nullable(false)
                .build();

        ParameterDescriptor baseBranchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Existing base branch to create the new branch from")
                .required(true)
                .example("main")
                .nullable(false)
                .build();

        ParameterDescriptor newBranchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Name of the new branch to be created")
                .required(true)
                .example("feature/x")
                .nullable(false)
                .build();

        // ==== Response Schema ====
        ParameterDescriptor responseSchema = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("Details of the created branch")
                .property("name", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("New branch name")
                        .required(true)
                        .build())
                .property("createdAt", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Timestamp of branch creation")
                        .required(true)
                        .build())
                .build();

        // ==== ToolDescriptor ====
        return ToolDescriptor.builder()
                .category("Repository")
                .name("CreateBranch")
                .displayName("Create Branch")
                .description("Creates a new branch from a specified base branch")
                .version("1.0.0")
                .tag("git")
                .tag("branch")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("baseBranch", baseBranchParam)
                .parameter("newBranchName", newBranchParam)
                .response(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseDescriptor.builder()
                        .responseSchema(responseSchema)
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("SUCCESS")
                                .description("Branch created successfully")
                                .contentType("application/json")
                                .build())
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("ERROR")
                                .description("An error occurred while creating the branch")
                                .contentType("application/json")
                                .build())
                        .example(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.Example.builder()
                                .title("Create branch example")
                                .description("Creates a new branch 'feature/x' from 'main'")
                                .input(Map.of(
                                        "repositoryId", "repo-001",
                                        "baseBranch", "main",
                                        "newBranchName", "feature/x"))
                                .output(Map.of(
                                        "name", "feature/x",
                                        "createdAt", "2025-11-23T23:00:00Z"))
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
        String baseBranch = VariableUtils.safeString(request.getParameters().get(BASE_BRANCH_KEY));
        String newBranchName = VariableUtils.safeString(request.getParameters().get(NEW_BRANCH_NAME_KEY));

        log.info("[{}] Creating branch: repo={}, base={}, new={}", toolId, repositoryId, baseBranch, newBranchName);

        return scmOperationService.createBranch(repositoryId, baseBranch, newBranchName, user)
                .map(b -> InvokeResponse.builder()
                        .requestId(requestId)
                        .toolId(toolId)
                        .result(b)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .requestId(requestId)
                            .toolId(toolId)
                            .error(InvokeResponse.ErrorInfo.builder()
                                    .code("CREATE_BRANCH_ERROR")
                                    .message(ToolExecutorUtil.buildErrorResponse(request.getToolId(), e))
                                    .stackTrace(Arrays.toString(e.getStackTrace()))
                                    .detail("repositoryId", repositoryId)
                                    .detail("baseBranch", baseBranch)
                                    .detail("newBranchName", newBranchName)
                                    .build())
                            .executedAt(Instant.now())
                            .build());
                });
    }
}