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
public class DeleteBranchTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(DeleteBranchTool.class);

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

        ParameterDescriptor branchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Branch to delete")
                .required(true)
                .example("feature/x")
                .build();

        return ToolDescriptor.builder()
                .category("Repository")
                .name("DeleteBranch")
                .displayName("Delete Branch")
                .description("Deletes a branch in the repository")
                .version("1.0.0")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("branch", branchParam)
                .status(ToolDescriptor.ToolDescriptorStatus.ACTIVE)
                .build();
    }

    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String requestId = request.getId();
        String toolId = request.getToolId();
        String repositoryId = (String) request.getParameters().get("repositoryId");
        String branch = (String) request.getParameters().get("branch");

        log.info("[{}] Deleting branch: repo={}, branch={}", toolId, repositoryId, branch);

        return scmOperationService.deleteBranch(repositoryId, branch, user)
                .map(r -> InvokeResponse.builder()
                        .id(requestId)
                        .toolId(toolId)
                        .result(r)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .id(requestId)
                            .toolId(request.getToolId())
                            .error(InvokeResponse.ErrorInfo.builder()
                                    .code("DELETE_BRANCH_ERROR")
                                    .message(ToolExecutorUtil.buildErrorResponse(request.getToolId(), e))
                                    .stackTrace(Arrays.toString(e.getStackTrace()))
                                    .detail("repositoryId", repositoryId)
                                    .detail("branch", branch)
                                    .build())
                            .executedAt(Instant.now())
                            .build());
                });
    }
}