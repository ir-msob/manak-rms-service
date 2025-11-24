package ir.msob.manak.rms.scm.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.rms.dto.Patch;
import ir.msob.manak.domain.model.rms.dto.ScmResult;
import ir.msob.manak.domain.model.workflow.WorkerExecutionStatus;
import ir.msob.manak.rms.camunda.CamundaService;
import ir.msob.manak.rms.scm.scmprovider.ScmOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

import static ir.msob.manak.domain.model.worker.Constants.WORKER_EXECUTION_ERROR_KEY;
import static ir.msob.manak.domain.model.worker.Constants.WORKER_EXECUTION_STATUS_KEY;
import static ir.msob.manak.rms.scm.worker.Constants.APPLY_PATCH_MESSAGE_KEY;

@Component
@RequiredArgsConstructor
public class ApplyPatchWorker {

    private static final Logger logger = LoggerFactory.getLogger(ApplyPatchWorker.class);

    private final UserService userService;
    private final CamundaService camundaService;
    private final IdService idService;
    private final ScmOperationService scmOperationService;

    @JobWorker(type = "apply-patch", autoComplete = false)
    public Mono<Void> execute(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();

        String repositoryId = (String) vars.get("repositoryId");
        String branch = (String) vars.get("branch");
        String patchRaw = (String) vars.get("patch");
        String commitMessage = (String) vars.get("commitMessage");

        logger.info("Starting 'apply-patch' job. jobKey={} repositoryId={} branch={}", job.getKey(), repositoryId, branch);

        String patchContent = decodePatch(patchRaw);

        Patch patch = Patch.builder()
                .diff(patchContent)
                .build();

        return scmOperationService.applyPatch(repositoryId, branch, patch, commitMessage, userService.getSystemUser())
                .doOnSuccess(result -> logger.info("Patch applied successfully. repositoryId={} branch={}", repositoryId, branch))
                .flatMap(this::prepareResult)
                .flatMap(result -> camundaService.complete(job, result))
                .doOnSuccess(v -> logger.info("Job completed successfully. jobKey={}", job.getKey()))
                .doOnError(ex -> logger.error("Job execution failed. jobKey={} error={}", job.getKey(), ex.getMessage(), ex))
                .onErrorResume(ex -> handleErrorAndReThrow(job, repositoryId, ex));
    }

    private String decodePatch(String patchRaw) {
        if (patchRaw == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(patchRaw);
            return new String(decoded);
        } catch (IllegalArgumentException e) {
            logger.warn("Patch is not Base64 encoded, using raw content");
            return patchRaw;
        }
    }

    private Mono<Map<String, Object>> prepareResult(ScmResult scmResult) {
        return Mono.just(Map.of(
                APPLY_PATCH_MESSAGE_KEY, scmResult.getMessage(),
                WORKER_EXECUTION_STATUS_KEY, WorkerExecutionStatus.SUCCESS
        ));
    }

    private Mono<Void> handleErrorAndReThrow(ActivatedJob job, String repositoryId, Throwable ex) {
        String errorMessage = "Apply patch job failed. repositoryId=" + repositoryId + " error=" + ex.getMessage();
        return camundaService.complete(job, prepareErrorResult(errorMessage))
                .then(Mono.error(ex));
    }

    private Map<String, Object> prepareErrorResult(String errorMessage) {
        return Map.of(
                WORKER_EXECUTION_STATUS_KEY, WorkerExecutionStatus.ERROR,
                WORKER_EXECUTION_ERROR_KEY, errorMessage
        );
    }
}
