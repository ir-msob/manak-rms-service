package ir.msob.manak.rms.scm.scmprovider.github;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.manak.domain.model.rms.dto.*;
import ir.msob.manak.rms.scm.scmprovider.ScmProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Full single-file implementation of ScmProviderService for GitHub (REST API v3).
 * <p>
 * Notes:
 * - This uses the "Contents" API to read/create/update files.
 * - applyPatch expects Patch.diff to be a JSON array like:
 * [
 * { "path": "src/Main.java", "content": "public class Main { ... }" },
 * { "path": "README.md", "content": "# Title\n..." }
 * ]
 * <p>
 * If you prefer another format for patch.diff, replace the parsing logic in applyPatch.
 */
@Service
@Slf4j
public class GithubProviderService implements ScmProviderService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GithubProviderService() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().followRedirect(true)
                ))
                .baseUrl("https://api.github.com")
                .build();
    }

    private static boolean isBase64Like(String s) {
        // simple heuristic: contains non-control chars but may contain '=' padding
        if (s.isEmpty()) return false;
        // if it's valid base64 bytes decodeable => true
        try {
            Base64.getDecoder().decode(s.replaceAll("\\s", ""));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // Helper to add auth & accept header
    private WebClient.RequestHeadersSpec<?> withAuth(WebClient.RequestHeadersSpec<?> req, ScmContext ctx) {
        return req.header("Authorization", "Bearer " + ctx.getAuthToken())
                .header("Accept", "application/vnd.github.v3+json");
    }

    // -----------------------
    // Repository Info
    // -----------------------
    @Override
    public Mono<Boolean> validateAccess(ScmContext context) {
        String repo = context.getRepository();
        log.info("🔐 [GitHub] validateAccess repo={}", repo);
        String url = "/repos/" + repo;

        return withAuth(webClient.get().uri(url), context)
                .retrieve()
                .toBodilessEntity()
                .map(e -> true)
                .onErrorResume(e -> {
                    log.warn("❌ [GitHub] validateAccess failed for {}: {}", repo, e.getMessage());
                    return Mono.just(false);
                });
    }

    // -----------------------
    // File Operations
    // -----------------------
    @Override
    public Mono<FileContent> readFile(ScmContext ctx, BranchRef branch, String filePath) {
        log.info("📄 [GitHub] readFile repo={}, branch={}, path={}", ctx.getRepository(), branch.getName(), filePath);
        String url = String.format("/repos/%s/contents/%s?ref=%s", ctx.getRepository(), filePath, branch.getName());

        return withAuth(webClient.get().uri(url), ctx)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> handleFileResponse(response, filePath))
                .doOnError(e -> log.error("❌ [GitHub] readFile error for {}: {}", filePath, e.getMessage()));
    }

    private Mono<FileContent> handleFileResponse(ClientResponse response, String filePath) {
        if (response.statusCode().is4xxClientError()) {
            return Mono.error(new RuntimeException("File not found or unauthorized: " + filePath));
        }
        return response.bodyToMono(GithubFileResponse.class)
                .map(r -> {
                    byte[] decoded = r.content == null ? new byte[0] : Base64.getDecoder().decode(r.content.replaceAll("\\s", ""));
                    return FileContent.builder()
                            .path(r.path)
                            .content(new String(decoded, StandardCharsets.UTF_8))
                            .build();
                });
    }

    @Override
    public Flux<DataBuffer> downloadArchive(ScmContext ctx, BranchRef branch) {
        log.info("📦 [GitHub] downloadArchive repo={}, branch={}", ctx.getRepository(), branch.getName());
        String url = String.format("/repos/%s/zipball/%s", ctx.getRepository(), branch.getName());

        return withAuth(webClient.get().uri(url), ctx)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .doOnError(e -> log.error("❌ [GitHub] downloadArchive failed: {}", e.getMessage()));
    }

    // -----------------------
    // Branch Management
    // -----------------------
    @Override
    public Mono<BranchRef> createBranch(ScmContext ctx, BranchRef baseBranch, String newBranchName) {
        String repo = ctx.getRepository();
        log.info("🌱 [GitHub] createBranch repo={}, base={}, new={}", repo, baseBranch.getName(), newBranchName);

        String refUrl = String.format("/repos/%s/git/ref/heads/%s", repo, baseBranch.getName());

        // 1) get SHA of base branch
        return withAuth(webClient.get().uri(refUrl), ctx)
                .retrieve()
                .bodyToMono(GithubRefResponse.class)
                .flatMap(refResp -> {
                    String sha = refResp.object.sha;
                    GithubCreateRefRequest req = new GithubCreateRefRequest("refs/heads/" + newBranchName, sha);
                    String createUrl = String.format("/repos/%s/git/refs", repo);
                    return withAuth(webClient.post().uri(createUrl).bodyValue(req), ctx)
                            .retrieve()
                            .toBodilessEntity()
                            .map(r -> new BranchRef(newBranchName, sha));
                })
                .onErrorMap(e -> {
                    log.error("❌ [GitHub] createBranch failed: {}", e.getMessage());
                    return e;
                });
    }

    // -----------------------
    // Patch & Commit
    // -----------------------

    @Override
    public Mono<ScmResult> deleteBranch(ScmContext ctx, BranchRef branch) {
        String repo = ctx.getRepository();
        log.info("🗑 [GitHub] deleteBranch repo={}, branch={}", repo, branch.getName());
        String url = String.format("/repos/%s/git/refs/heads/%s", repo, branch.getName());

        return withAuth(webClient.delete().uri(url), ctx)
                .retrieve()
                .toBodilessEntity()
                .map(e -> new ScmResult("Branch deleted: " + branch.getName()))
                .onErrorResume(e -> {
                    log.error("❌ [GitHub] deleteBranch failed: {}", e.getMessage());
                    return Mono.just(new ScmResult(e.getMessage()));
                });
    }

    /**
     * applyPatch expects PATCH.diff to be a JSON array:
     * [
     * { "path": "src/Main.java", "content": "plain file content or base64:" },
     * ...
     * ]
     * <p>
     * For each entry we will:
     * - GET existing file (to fetch sha if exists)
     * - PUT /repos/{repo}/contents/{path} with { message, content(base64), branch, sha? }
     */
    @Override
    public Mono<ScmResult> applyPatch(ScmContext ctx, BranchRef branch, Patch patch, String commitMessage) {
        String repo = ctx.getRepository();
        log.info("🩹 [GitHub] applyPatch repo={}, branch={} commitMessage={}", repo, branch.getName(), commitMessage);

        // Parse patch.diff as JSON array of { path, content }
        List<Map<String, Object>> files;
        try {
            files = objectMapper.readValue(patch.getDiff(), new TypeReference<>() {
            });
        } catch (Exception e) {
            String msg = "applyPatch: failed to parse patch.diff as JSON array of {path,content}";
            log.error("❌ [GitHub] {} - {}", msg, e.getMessage());
            return Mono.just(new ScmResult(msg + ": " + e.getMessage()));
        }

        // Apply files sequentially
        return Flux.fromIterable(files)
                .concatMap(fileObj -> {
                    String path = String.valueOf(fileObj.get("path"));
                    Object contentObj = fileObj.get("content");
                    if (path == null || contentObj == null) {
                        return Mono.error(new RuntimeException("Invalid file entry, missing path or content"));
                    }
                    String rawContent = String.valueOf(contentObj);

                    // If content looks base64 (heuristic: contains non-printable or many "=" at end), allow as-is
                    boolean looksBase64 = isBase64Like(rawContent);

                    String base64Content = looksBase64 ? rawContent : Base64.getEncoder().encodeToString(rawContent.getBytes(StandardCharsets.UTF_8));

                    // Try to GET existing file to obtain sha
                    String getUrl = String.format("/repos/%s/contents/%s?ref=%s", repo, path, branch.getName());

                    return withAuth(webClient.get().uri(getUrl), ctx)
                            .retrieve()
                            .onStatus(status -> status.value() == 404, resp -> Mono.empty()) // we'll handle not found by switching to create
                            .bodyToMono(GithubFileResponse.class)
                            .flatMap(existing -> {
                                // Update existing file (need sha)
                                GithubCreateUpdateFileRequest req = new GithubCreateUpdateFileRequest(commitMessage, base64Content, branch.getName(), existing.sha);
                                String putUrl = String.format("/repos/%s/contents/%s", repo, path);
                                return withAuth(webClient.put().uri(putUrl).bodyValue(req), ctx)
                                        .retrieve()
                                        .toBodilessEntity()
                                        .map(r -> "updated:" + path)
                                        .onErrorMap(e -> new RuntimeException("update failed for " + path + ": " + e.getMessage()));
                            })
                            .onErrorResume(e -> {
                                // if GET failed (404 or other), attempt create
                                // create
                                // fallback: attempt create (PUT without sha)
                                GithubCreateUpdateFileRequest req = new GithubCreateUpdateFileRequest(commitMessage, base64Content, branch.getName(), null);
                                String putUrl = String.format("/repos/%s/contents/%s", repo, path);
                                return withAuth(webClient.put().uri(putUrl).bodyValue(req), ctx)
                                        .retrieve()
                                        .toBodilessEntity()
                                        .map(r -> "created:" + path)
                                        .onErrorMap(err -> new RuntimeException("create failed for " + path + ": " + err.getMessage()));
                            })
                            .onErrorResume(err -> {
                                // Some GitHub error mapping -> wrap as error to stop the chain
                                return Mono.error(new RuntimeException("applyPatch failed for " + path + ": " + err.getMessage()));
                            });
                })
                .collectList()
                .map(results -> new ScmResult("Applied patch to " + results.size() + " files: " + String.join(", ", results)))
                .onErrorResume(e -> {
                    log.error("❌ [GitHub] applyPatch failed: {}", e.getMessage());
                    return Mono.just(new ScmResult(e.getMessage()));
                });
    }

    // -----------------------
    // Pull / Merge Request
    // -----------------------
    @Override
    public Mono<PullRequestInfo> createPullRequest(ScmContext ctx, BranchRef sourceBranch, BranchRef targetBranch, String title, String description) {
        String repo = ctx.getRepository();
        log.info("🔀 [GitHub] createPullRequest repo={}, from={} to={} title={}", repo, sourceBranch.getName(), targetBranch.getName(), title);

        GithubCreatePrRequest req = new GithubCreatePrRequest(title, description, sourceBranch.getName(), targetBranch.getName());
        String url = String.format("/repos/%s/pulls", repo);

        return withAuth(webClient.post().uri(url).bodyValue(req), ctx)
                .retrieve()
                .bodyToMono(GithubPrResponse.class)
                .map(resp -> new PullRequestInfo(
                        String.valueOf(resp.number),
                        resp.title,
                        resp.body,
                        resp.head.ref,
                        resp.base.ref,
                        PullRequestInfo.PullRequestStatus.OPEN,
                        resp.user == null ? null : resp.user.login,
                        resp.html_url,
                        Instant.ofEpochSecond(resp.created_at),
                        Instant.ofEpochSecond(resp.updated_at)
                ))
                .onErrorResume(e -> {
                    log.error("❌ [GitHub] createPullRequest failed: {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<MergeResult> mergePullRequest(ScmContext ctx, String pullRequestId) {
        String repo = ctx.getRepository();
        log.info("🔀 [GitHub] mergePullRequest repo={}, pr={}", repo, pullRequestId);
        String url = String.format("/repos/%s/pulls/%s/merge", repo, pullRequestId);

        return withAuth(webClient.put().uri(url), ctx)
                .retrieve()
                .bodyToMono(GithubMergeResponse.class)
                .map(r -> new MergeResult(true, pullRequestId, r.sha, "Merged", MergeResult.MergeFailureReason.NONE))
                .onErrorResume(e -> {
                    log.error("❌ [GitHub] mergePullRequest failed: {}", e.getMessage());
                    return Mono.just(new MergeResult(false, pullRequestId, null, e.getMessage(), MergeResult.MergeFailureReason.UNKNOWN));
                });
    }

    @Override
    public Mono<ScmResult> closePullRequest(ScmContext ctx, String pullRequestId) {
        String repo = ctx.getRepository();
        log.info("🔒 [GitHub] closePullRequest repo={}, pr={}", repo, pullRequestId);
        String url = String.format("/repos/%s/pulls/%s", repo, pullRequestId);

        return withAuth(webClient.patch().uri(url).bodyValue(Map.of("state", "closed")), ctx)
                .retrieve()
                .toBodilessEntity()
                .map(r -> new ScmResult("Pull request closed: " + pullRequestId))
                .onErrorResume(e -> {
                    log.error("❌ [GitHub] closePullRequest failed: {}", e.getMessage());
                    return Mono.just(new ScmResult(e.getMessage()));
                });
    }

    // -----------------------
    // CI/CD Pipeline (GitHub Actions)
    // -----------------------
    @Override
    public Mono<PipelineResult> triggerPipeline(ScmContext ctx, PipelineSpec spec) {
        String repo = ctx.getRepository();
        log.info("🚀 [GitHub] triggerPipeline repo={}, workflowId/ref={}, branch={}", repo, spec.getTriggerSource(), spec.getBranch());

        String url = String.format("/repos/%s/actions/workflows/%s/dispatches", repo, spec.getTriggerSource());

        Map<String, Object> body = Map.of(
                "ref", spec.getBranch(),
                "inputs", spec.getVariables() == null ? Map.of() : spec.getVariables()
        );

        return withAuth(webClient.post().uri(url).bodyValue(body), ctx)
                .retrieve()
                .toBodilessEntity()
                .map(e -> new PipelineResult(null, PipelineResult.PipelineStatus.QUEUED, "Workflow dispatch requested", Instant.now(), null, null))
                .onErrorResume(ex -> {
                    log.error("❌ [GitHub] triggerPipeline failed: {}", ex.getMessage());
                    return Mono.just(new PipelineResult(null, PipelineResult.PipelineStatus.FAILED, ex.getMessage(), Instant.now(), Instant.now(), null));
                });
    }

    // -----------------------
    // GitHub DTOs (internal)
    // -----------------------
    private static class GithubFileResponse {
        public String path;
        public String content;
        public String sha;
    }

    private static class GithubRefResponse {
        public GithubRefObject object;
    }

    private static class GithubRefObject {
        public String sha;
    }

    private record GithubCreateRefRequest(String ref, String sha) {
    }

    /**
     * @param sha optional for update
     */
    private record GithubCreateUpdateFileRequest(String message, String content, String branch, String sha) {
    }

    private static class GithubPrResponse {
        public int number;
        public String title;
        public String body;
        public GithubPrBranch head;
        public GithubPrBranch base;
        public GithubUser user;
        public String html_url;
        public long created_at;
        public long updated_at;

        // helper
    }

    private static class GithubPrBranch {
        public String label;
        public String ref;
        public String sha;
    }

    private static class GithubUser {
        public String login;
    }

    private record GithubCreatePrRequest(String title, String body, String head, String base) {
    }

    private static class GithubMergeResponse {
        public boolean merged;
        public String message;
        public String sha;
    }
}
