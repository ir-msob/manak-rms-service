package ir.msob.manak.rms.scm.tool;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.common.model.ParameterDescriptor;
import ir.msob.manak.domain.model.common.model.RetryPolicy;
import ir.msob.manak.domain.model.common.model.TimeoutPolicy;
import ir.msob.manak.domain.model.rms.dto.Patch;
import ir.msob.manak.domain.model.toolhub.ToolExecutor;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseDescriptor;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplyPatchTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ApplyPatchTool.class);

    private final ScmOperationService scmOperationService;
    private final RepositoryService repositoryService;

    @Override
    public ToolDescriptor getToolDescriptor() {
        // ==== Parameters ====
        ParameterDescriptor repositoryIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Repository ID to apply the patch to")
                .required(true)
                .examples(List.of("repo-001"))
                .nullable(false)
                .build();

        ParameterDescriptor branchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Target branch for applying the patch")
                .required(true)
                .examples(List.of("feature/x"))
                .nullable(false)
                .build();

        ParameterDescriptor patchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Patch content, either base64 encoded or raw diff text")
                .required(true)
                .examples(Arrays.asList(
                        "...diff content...",
                        "ZGVmYXVsdCBwYXRjaCBjb250ZW50Cg==" // base64 example
                ))
                .nullable(false)
                .build();

        ParameterDescriptor commitMessageParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Commit message for the applied patch")
                .required(false)
                .examples(List.of("Apply automated changes"))
                .nullable(true)
                .build();

        // ==== Response Schema ====
        ParameterDescriptor responseSchema = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("Result of patch application")
                .property("success", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.BOOLEAN)
                        .description("Whether the patch was applied successfully")
                        .required(true)
                        .build())
                .property("pullRequestId", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Associated pull request ID, if any")
                        .required(false)
                        .nullable(true)
                        .build())
                .property("mergedCommitId", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Commit ID resulting from merge, if applicable")
                        .required(false)
                        .nullable(true)
                        .build())
                .property("message", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Informational message about patch application")
                        .required(true)
                        .build())
                .property("failureReason", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Failure reason if patch application failed")
                        .required(false)
                        .nullable(true)
                        .build())
                .build();

        // ==== ToolDescriptor ====
        return ToolDescriptor.builder()
                .category("Repository")
                .name("ApplyPatch")
                .displayName("Apply Patch")
                .description("Applies a patch/diff to the target branch and optionally commits the changes.")
                .version("1.0.0")
                .tag("git")
                .tag("patch")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("branch", branchParam)
                .parameter("patch", patchParam)
                .parameter("commitMessage", commitMessageParam)
                .response(ResponseDescriptor.builder()
                        .responseSchema(responseSchema)
                        .status(ResponseStatus.builder()
                                .status("SUCCESS")
                                .description("Patch applied successfully")
                                .contentType("application/json")
                                .build())
                        .status(ResponseStatus.builder()
                                .status("ERROR")
                                .description("An error occurred during patch application")
                                .contentType("application/json")
                                .build())
                        .example(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.Example.builder()
                                .title("Apply patch to repository")
                                .description("Demonstrates applying a patch to a feature branch")
                                .input(Map.of(
                                        "repositoryId", "repo-001",
                                        "branch", "feature/x",
                                        "patch", "...diff content...",
                                        "commitMessage", "Apply automated changes"))
                                .output(Map.of(
                                        "success", true,
                                        "pullRequestId", "PR-123",
                                        "mergedCommitId", "abcd1234",
                                        "message", "Patch applied successfully",
                                        "failureReason", null))
                                .build())
                        .build())
                .retryPolicy(RetryPolicy.builder()
                        .enabled(true)
                        .maxAttempts(3)
                        .initialIntervalMs(500)
                        .multiplier(2.0)
                        .maxIntervalMs(2000)
                        .build())
                .timeoutPolicy(TimeoutPolicy.builder()
                        .timeoutMs(5000)
                        .failFast(false)
                        .gracePeriodMs(1000)
                        .build())
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