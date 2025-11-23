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
public class MergePullRequestTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(MergePullRequestTool.class);

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

        ParameterDescriptor prIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Pull Request ID")
                .required(true)
                .example("42")
                .build();

        return ToolDescriptor.builder()
                .category("Repository")
                .name("MergePullRequest")
                .displayName("Merge Pull Request")
                .description("Merges a pull request by id")
                .version("1.0.0")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("pullRequestId", prIdParam)
                .status(ToolDescriptor.ToolDescriptorStatus.ACTIVE)
                .build();
    }

    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String requestId = request.getId();
        String toolId = request.getToolId();
        String repositoryId = (String) request.getParameters().get("repositoryId");
        String prId = (String) request.getParameters().get("pullRequestId");

        log.info("[{}] Merging PR: repo={}, prId={}", toolId, repositoryId, prId);

        return scmOperationService.mergePullRequest(repositoryId, prId, user)
                .map(res -> InvokeResponse.builder()
                        .id(requestId)
                        .toolId(toolId)
                        .result(res)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error merging PR", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .id(requestId)
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