package ir.msob.manak.rms.scm.tool;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.common.model.ParameterDescriptor;
import ir.msob.manak.domain.model.toolhub.ToolExecutor;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ToolDescriptor;
import ir.msob.manak.domain.service.toolhub.util.ToolExecutorUtil;
import ir.msob.manak.rms.repository.RepositoryService;
import ir.msob.manak.rms.scm.scmprovider.ScmOperationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class CreateBranchTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(CreateBranchTool.class);

    private final ScmOperationService scmOperationService;
    private final RepositoryService repositoryService;

    @Override
    public ToolDescriptor getToolDescriptor() {
        ParameterDescriptor repositoryIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Repository ID")
                .required(true)
                .example("repo-001")
                .build();

        ParameterDescriptor baseBranchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Base branch")
                .required(true)
                .example("main")
                .build();

        ParameterDescriptor newBranchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("New branch name")
                .required(true)
                .example("feature/x")
                .build();

        return ToolDescriptor.builder()
                .category("Repository")
                .name("CreateBranch")
                .displayName("Create Branch")
                .description("Creates a new branch from a base branch")
                .version("1.0.0")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("baseBranch", baseBranchParam)
                .parameter("newBranchName", newBranchParam)
                .status(ToolDescriptor.ToolDescriptorStatus.ACTIVE)
                .build();
    }

    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String requestId = request.getId();
        String toolId = request.getToolId();
        String repositoryId = (String) request.getParameters().get("repositoryId");
        String baseBranch = (String) request.getParameters().get("baseBranch");
        String newBranchName = (String) request.getParameters().get("newBranchName");

        log.info("[{}] Creating branch: repo={}, base={}, new={}", toolId, repositoryId, baseBranch, newBranchName);

        return scmOperationService.createBranch(repositoryId, baseBranch, newBranchName, user)
                .map(b -> InvokeResponse.builder()
                        .id(requestId)
                        .toolId(toolId)
                        .result(b)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .id(requestId)
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