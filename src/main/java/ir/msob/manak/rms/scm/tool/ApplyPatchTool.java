package ir.msob.manak.rms.scm.tool;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.common.model.ParameterDescriptor;
import ir.msob.manak.domain.model.rms.dto.Patch;
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
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ApplyPatchTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ApplyPatchTool.class);

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
                .description("Target branch")
                .required(true)
                .example("feature/x")
                .build();

        ParameterDescriptor patchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Patch content (base64 encoded or raw diff text)")
                .required(true)
                .example("...diff...")
                .build();

        ParameterDescriptor commitMessageParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Commit message")
                .required(false)
                .example("Apply automated changes")
                .build();

        return ToolDescriptor.builder()
                .category("Repository")
                .name("ApplyPatch")
                .displayName("Apply Patch")
                .description("Applies a patch/diff to the target branch and optionally commits.")
                .version("1.0.0")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("branch", branchParam)
                .parameter("patch", patchParam)
                .parameter("commitMessage", commitMessageParam)
                .status(ToolDescriptor.ToolDescriptorStatus.ACTIVE)
                .build();
    }

    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String requestId = request.getId();
        String toolId = request.getToolId();
        String repositoryId = (String) request.getParameters().get("repositoryId");
        String branch = (String) request.getParameters().get("branch");
        String patchRaw = (String) request.getParameters().get("patch");
        String commitMessage = (String) request.getParameters().get("commitMessage");

        log.info("[{}] Applying patch: repo={}, branch={}", toolId, repositoryId, branch);

        // try base64 decode if looks encoded
        String patchContent = patchRaw;
        try {
            if (patchRaw != null && Base64.getDecoder().decode(patchRaw).length > 0) {
                // if decodable and when re-encoded equals original length threshold, accept it
                patchContent = new String(Base64.getDecoder().decode(patchRaw));
            }
        } catch (IllegalArgumentException ignored) {
            // keep original
        }

        // build Patch DTO - here we assume Patch has constructor from String or factory
        Patch patch = Patch.builder()
                .diff(patchContent)
                .build(); // <- replace with your actual constructor/factory

        return scmOperationService.applyPatch(repositoryId, branch, patch, commitMessage, user)
                .map(r -> InvokeResponse.builder()
                        .id(requestId)
                        .toolId(toolId)
                        .result(r)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error applying patch", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .id(requestId)
                            .toolId(toolId)
                            .error(InvokeResponse.ErrorInfo.builder()
                                    .code("APPLY_PATCH_ERROR")
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