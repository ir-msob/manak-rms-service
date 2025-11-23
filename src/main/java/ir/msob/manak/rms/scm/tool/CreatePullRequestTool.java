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
public class CreatePullRequestTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(CreatePullRequestTool.class);

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

        ParameterDescriptor sourceBranch = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Source branch")
                .required(true)
                .example("feature/x")
                .build();

        ParameterDescriptor targetBranch = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Target branch")
                .required(true)
                .example("main")
                .build();

        ParameterDescriptor title = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("PR title")
                .required(true)
                .example("Add feature X")
                .build();

        ParameterDescriptor description = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("PR description")
                .required(false)
                .example("This PR introduces ...")
                .build();

        return ToolDescriptor.builder()
                .category("Repository")
                .name("CreatePullRequest")
                .displayName("Create Pull Request")
                .description("Creates a pull/merge request from source to target branch")
                .version("1.0.0")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("sourceBranch", sourceBranch)
                .parameter("targetBranch", targetBranch)
                .parameter("title", title)
                .parameter("description", description)
                .status(ToolDescriptor.ToolDescriptorStatus.ACTIVE)
                .build();
    }

    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String requestId = request.getId();
        String toolId = request.getToolId();
        String repositoryId = (String) request.getParameters().get("repositoryId");
        String src = (String) request.getParameters().get("sourceBranch");
        String tgt = (String) request.getParameters().get("targetBranch");
        String title = (String) request.getParameters().get("title");
        String description = (String) request.getParameters().get("description");

        log.info("[{}] Creating PR: repo={}, {} -> {}", toolId, repositoryId, src, tgt);

        return scmOperationService.createPullRequest(repositoryId, src, tgt, title, description, user)
                .map(pr -> InvokeResponse.builder()
                        .id(requestId)
                        .toolId(toolId)
                        .result(pr)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error creating PR", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .id(requestId)
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