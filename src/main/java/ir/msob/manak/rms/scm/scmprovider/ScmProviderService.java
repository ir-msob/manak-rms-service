package ir.msob.manak.rms.scm.scmprovider;

import ir.msob.manak.domain.model.rms.dto.*;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * High-level, vendor-agnostic Source Control Management service.
 * Designed for GitHub / GitLab / Bitbucket / Azure DevOps / Local.
 */
public interface ScmProviderService {

    // =====================
    // Repository Info
    // =====================

    /**
     * Checks if a repository is accessible and authenticated.
     */
    Mono<Boolean> validateAccess(ScmContext context);


    // =====================
    // File Operations
    // =====================

    /**
     * Reads a file from the repository at a given branch.
     */
    Mono<FileContent> readFile(ScmContext ctx, BranchRef branch, String filePath);

    /**
     * Downloads repository snapshot (zip, tar, etc.).
     */
    Flux<DataBuffer> downloadArchive(ScmContext ctx, BranchRef branch);


    // =====================
    // Branch Management
    // =====================

    /**
     * Creates a new branch from a base branch.
     */
    Mono<BranchRef> createBranch(ScmContext ctx, BranchRef baseBranch, String newBranchName);

    /**
     * Deletes a branch.
     */
    Mono<ScmResult> deleteBranch(ScmContext ctx, BranchRef branch);


    // =====================
    // Patch & Commit
    // =====================

    /**
     * Applies a patch (diff) and optionally commits it.
     */
    Mono<ScmResult> applyPatch(ScmContext ctx, BranchRef branch, Patch patch, String commitMessage);


    // =====================
    // Pull / Merge Request
    // =====================

    Mono<PullRequestInfo> createPullRequest(ScmContext ctx, BranchRef sourceBranch, BranchRef targetBranch, String title, String description);

    Mono<MergeResult> mergePullRequest(ScmContext ctx, String pullRequestId);

    Mono<ScmResult> closePullRequest(ScmContext ctx, String pullRequestId);


    // =====================
    // CI/CD Pipeline
    // =====================

    /**
     * Triggers a pipeline/run for the current repository.
     */
    Mono<PipelineResult> triggerPipeline(ScmContext ctx, PipelineSpec spec);
}
