package ir.msob.manak.rms.scm.scmprovider;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.rms.dto.*;
import ir.msob.manak.rms.repository.RepositoryService;
import ir.msob.manak.rms.util.RepositoryUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ScmOperationService {

    private static final Logger log = LoggerFactory.getLogger(ScmOperationService.class);

    private final RepositoryService repositoryService;
    private final ScmProviderRegistry scmProviderRegistry;

    /**
     * Helper to log and rethrow errors.
     */
    private <T> Mono<T> handleError(String message, Throwable e) {
        log.error(message, e);
        return Mono.error(e);
    }

    private <T> Flux<T> handleErrorFlux(String message, Throwable e) {
        log.error(message, e);
        return Flux.error(e);
    }


    // ============================================================
    // Simple CRUD operations
    // ============================================================

    public Mono<FileContent> readFile(String repositoryId, String branch, String filePath, User user) {
        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .readFile(ctx, RepositoryUtil.getBuild(branch), filePath);
                })
                .onErrorResume(e -> handleError("Error in readFile()", e));
    }


    public Flux<DataBuffer> downloadArchive(String repositoryId, String branch, User user) {
        return repositoryService.getOne(repositoryId, user)
                .flatMapMany(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .downloadArchive(ctx, RepositoryUtil.getBuild(branch));
                })
                .onErrorResume(e -> handleErrorFlux("Error in downloadArchive()", e));
    }


    public Mono<BranchRef> createBranch(String repositoryId, String baseBranch, String newBranchName, User user) {
        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .createBranch(ctx, RepositoryUtil.getBuild(baseBranch), newBranchName);
                })
                .onErrorResume(e -> handleError("Error in createBranch()", e));
    }


    public Mono<ScmResult> deleteBranch(String repositoryId, String branch, User user) {
        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .deleteBranch(ctx, RepositoryUtil.getBuild(branch));
                })
                .onErrorResume(e -> handleError("Error in deleteBranch()", e));
    }


    public Mono<ScmResult> applyPatch(
            String repositoryId,
            String branch,
            Patch patch,
            String commitMessage,
            User user
    ) {
        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .applyPatch(ctx, RepositoryUtil.getBuild(branch), patch, commitMessage);
                })
                .onErrorResume(e -> handleError("Error in applyPatch()", e));
    }


    public Mono<PullRequestInfo> createPullRequest(
            String repositoryId,
            String sourceBranch,
            String targetBranch,
            String title,
            String description,
            User user
    ) {
        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .createPullRequest(
                                    ctx,
                                    RepositoryUtil.getBuild(sourceBranch),
                                    RepositoryUtil.getBuild(targetBranch),
                                    title,
                                    description
                            );
                })
                .onErrorResume(e -> handleError("Error in createPullRequest()", e));
    }


    public Mono<MergeResult> mergePullRequest(String repositoryId, String pullRequestId, User user) {
        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .mergePullRequest(ctx, pullRequestId);
                })
                .onErrorResume(e -> handleError("Error in mergePullRequest()", e));
    }


    public Mono<ScmResult> closePullRequest(String repositoryId, String pullRequestId, User user) {
        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .closePullRequest(ctx, pullRequestId);
                })
                .onErrorResume(e -> handleError("Error in closePullRequest()", e));
    }


    public Mono<PipelineResult> triggerPipeline(String repositoryId, PipelineSpec spec, User user) {
        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    ScmContext ctx = RepositoryUtil.getScmContext(repo);
                    return scmProviderRegistry.getProvider(repo)
                            .triggerPipeline(ctx, spec);
                })
                .onErrorResume(e -> handleError("Error in triggerPipeline()", e));
    }
}
