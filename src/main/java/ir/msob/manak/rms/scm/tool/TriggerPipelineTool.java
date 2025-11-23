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
        // ==== Parameters ====
        ParameterDescriptor repositoryIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Repository ID where the pipeline will be triggered")
                .required(true)
                .example("repo-001")
                .nullable(false)
                .build();

        ParameterDescriptor pipelineSpecParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("Pipeline specification object (provider-specific)")
                .required(true)
                .example(Map.of("pipeline", "build-and-test"))
                .nullable(false)
                .build();

        // ==== Response Schema ====
        ParameterDescriptor responseSchema = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("Result of pipeline trigger")
                .property("pipelineId", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Triggered pipeline ID")
                        .required(true)
                        .build())
                .property("status", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("Pipeline status (e.g., triggered, running)")
                        .required(true)
                        .build())
                .build();

        // ==== ToolDescriptor ====
        return ToolDescriptor.builder()
                .category("CI/CD")
                .name("TriggerPipeline")
                .displayName("Trigger CI/CD Pipeline")
                .description("Triggers a CI/CD pipeline for the specified repository")
                .version("1.0.0")
                .tag("pipeline")
                .tag("ci/cd")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("spec", pipelineSpecParam)
                .response(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseDescriptor.builder()
                        .responseSchema(responseSchema)
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("SUCCESS")
                                .description("Pipeline triggered successfully")
                                .contentType("application/json")
                                .build())
                        .status(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus.builder()
                                .status("ERROR")
                                .description("Error occurred while triggering pipeline")
                                .contentType("application/json")
                                .build())
                        .example(ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.Example.builder()
                                .title("Trigger Pipeline Example")
                                .description("Triggers the build-and-test pipeline in repo-001")
                                .input(Map.of(
                                        "repositoryId", "repo-001",
                                        "spec", Map.of("pipeline", "build-and-test")))
                                .output(Map.of(
                                        "pipelineId", "pipeline-123",
                                        "status", "triggered"))
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