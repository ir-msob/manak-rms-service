package ir.msob.manak.rms.gitprovider.github;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.rms.dto.FileContentDto;
import ir.msob.manak.rms.gitprovider.GitProviderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class GithubProviderService implements GitProviderService {
    private static final Logger log = LoggerFactory.getLogger(GithubProviderService.class);

    private final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
            .build();

    @Override
    public Mono<FileContentDto> downloadFile(String repositoryPath, String branch, String filePath, String token, User user) {
        log.info("📂 [GitHub] Starting downloadFile | repoPath={}, branch={}, filePath={}, user={}",
                repositoryPath, branch, filePath, user.getUsername());

        String apiUrl = String.format("%s/contents/%s?ref=%s", repositoryPath, filePath, branch);
        log.debug("🔗 [GitHub] API URL: {}", apiUrl);

        return webClient
                .get()
                .uri(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    if (clientResponse.statusCode().value() == 404) {
                        log.warn("⚠️ [GitHub] File not found: {}", filePath);
                        return Mono.error(new CommonRuntimeException("File not found: {}", filePath));
                    }
                    log.error("🚫 [GitHub] Unauthorized or bad request while fetching file: {}", filePath);
                    return Mono.error(new CommonRuntimeException("Unauthorized or bad request for file: {}", filePath));
                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    log.error("🔥 [GitHub] Server error while fetching file: {}", filePath);
                    return Mono.error(new CommonRuntimeException("GitHub server error while fetching file: {}", filePath));
                })
                .onStatus(HttpStatusCode::is3xxRedirection, response -> {
                    log.error("🔁 [GitHub] Unexpected redirect for file: {}", filePath);
                    return Mono.error(new CommonRuntimeException("Unexpected redirect from GitHub for file: {}", filePath));
                })
                .bodyToMono(FileContentDto.class)
                .map(response -> {
                    log.debug("✅ [GitHub] File metadata received: name={}, encoding={}", response.getPath(), response.getEncoding());
                    String decoded = decodeContent(response.getContent(), response.getEncoding());
                    response.setContent(decoded);
                    response.setEncoding(null);
                    return response;
                })
                .doOnSubscribe(s -> log.info("⬇️ [GitHub] Download started for file {}", filePath))
                .doOnSuccess(f -> log.info("✅ [GitHub] File download completed: {}", f.getPath()))
                .doOnError(e -> log.error("❌ [GitHub] Error while downloading file {}: {}", filePath, e.getMessage(), e))
                .doFinally(signal -> log.info("🔚 [GitHub] Finished downloadFile for {} [signal={}]", filePath, signal));
    }

    @Override
    public Flux<FileContentDto> getMethodUsage(String repoUrl, String branch, String filePath, String method) {
        return Flux.empty();
    }

    @Override
    public Flux<FileContentDto> getClassUsage(String repoUrl, String branch, String filePath, String className) {
        return Flux.empty();
    }

    @Override
    public Flux<DataBuffer> downloadBranch(String repositoryPath, String branch, String token, User user) {
        log.info("📦 [GitHub] Starting downloadBranch | repoPath={}, branch={}, user={}",
                repositoryPath, branch, user.getUsername());

        String apiUrl = String.format("%s/zipball/%s", repositoryPath, branch);
        log.debug("🔗 [GitHub] API URL: {}", apiUrl);

        return webClient
                .get()
                .uri(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    log.warn("⚠️ [GitHub] Branch not found or unauthorized: {}", branch);
                    return Mono.error(new CommonRuntimeException("Branch not found or unauthorized: {}", branch));
                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    log.error("🔥 [GitHub] Server error while fetching branch: {}", branch);
                    return Mono.error(new CommonRuntimeException("GitHub server error while fetching branch: {}", branch));
                })
                .onStatus(HttpStatusCode::is3xxRedirection, response -> {
                    log.error("🔁 [GitHub] Unexpected redirect for branch: {}", branch);
                    return Mono.error(new CommonRuntimeException("Unexpected redirect from GitHub"));
                })
                .bodyToFlux(DataBuffer.class)
                .doOnSubscribe(s -> log.info("⬇️ [GitHub] Download started for branch {}", branch))
                .doOnNext(buf -> log.trace("📄 [GitHub] Received data buffer chunk ({} bytes)", buf.readableByteCount()))
                .doOnError(e -> log.error("❌ [GitHub] Error while downloading branch {}: {}", branch, e.getMessage(), e))
                .doFinally(signal -> log.info("🔚 [GitHub] Finished downloadBranch for {} [signal={}]", branch, signal));
    }

    private String decodeContent(String content, String encoding) {
        if ("base64".equals(encoding) && content != null) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(content.replace("\n", ""));
                return new String(decodedBytes, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                log.error("⚠️ [GitHub] Failed to decode Base64 content: {}", e.getMessage());
                throw new CommonRuntimeException("Failed to decode Base64 content");
            }
        }
        return content;
    }
}
