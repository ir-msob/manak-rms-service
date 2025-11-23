package ir.msob.manak.rms.scm.tool;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.common.model.ParameterDescriptor;
import ir.msob.manak.domain.model.rms.dto.PipelineSpec;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TriggerPipelineTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(TriggerPipelineTool.class);

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

        ParameterDescriptor pipelineSpecParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("Pipeline specification object (provider specific)")
                .required(true)
                .example(Map.of("pipeline", "build-and-test"))
                .build();

        return ToolDescriptor.builder()
                .category("CI/CD")
                .name("TriggerPipeline")
                .displayName("Trigger CI/CD Pipeline")
                .description("Triggers CI/CD pipeline for the repository")
                .version("1.0.0")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("spec", pipelineSpecParam)
                .status(ToolDescriptor.ToolDescriptorStatus.ACTIVE)
                .build();
    }

    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String requestId = request.getId();
        String toolId = request.getToolId();
        String repositoryId = (String) request.getParameters().get("repositoryId");
        PipelineSpec spec = (PipelineSpec) request.getParameters().get("spec");

        log.info("[{}] Trigger pipeline: repo={}, spec={}", toolId, repositoryId, spec);

        return scmOperationService.triggerPipeline(repositoryId, spec, user)
                .map(res -> InvokeResponse.builder()
                        .id(requestId)
                        .toolId(toolId)
                        .result(res)
                        .executedAt(Instant.now())
                        .build())
                .onErrorResume(e -> {
                    log.error("[{}] Error triggering pipeline", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .id(requestId)
                            .toolId(request.getToolId())
                            .error(InvokeResponse.ErrorInfo.builder()
                                    .code("TRIGGER_PIPELINE_ERROR")
                                    .message(ToolExecutorUtil.buildErrorResponse(request.getToolId(), e))
                                    .stackTrace(Arrays.toString(e.getStackTrace()))
                                    .detail("repositoryId", repositoryId)
                                    .build())
                            .executedAt(Instant.now())
                            .build());
                });
    }
}