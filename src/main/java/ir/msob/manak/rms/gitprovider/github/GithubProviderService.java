package ir.msob.manak.rms.gitprovider.github;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.rms.dto.FileContentDto;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import ir.msob.manak.rms.gitprovider.GitProviderService;
import lombok.RequiredArgsConstructor;
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
    private final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create().followRedirect(true)
            ))
            .build();

    @Override
    public Mono<FileContentDto> getFileContent(String repoUrl, String branch, String filePath) {
//        return getGitSpecification(repoUrl)
//                .flatMap(gitSpecification -> {
//                    String apiUrl = getApiUrl(gitSpecification, repoUrl, filePath);
//                    return webClient
//                            .get()
//                            .uri(apiUrl)
//                            .header("Accept", "application/vnd.github.v3+json")
//                            .header("Authorization", "token " + gitSpecification.getToken())
//                            .retrieve()
//                            .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
//                                if (clientResponse.statusCode().value() == 404) {
//                                    throw new CommonRuntimeException("File not found {}", filePath);
//                                }
//                                return Mono.error(new CommonRuntimeException("Error"));
//                            })
//                            .bodyToMono(GitHubFileContentResponse.class)
//                            .map(response -> FileContentDto.builder()
//                                    .repoUrl(repoUrl)
//                                    .filePath(filePath)
//                                    .branch(branch)
//                                    .content(decodeContent(response.getContent(), response.getEncoding()))
//                                    .build());
//                });
        return Mono.empty();
    }

    @Override
    public Flux<FileContentDto> getMethodUsage(String repoUrl, String branch, String filePath, String method) {
        return null;
    }

    @Override
    public Flux<FileContentDto> getClassUsage(String repoUrl, String branch, String filePath, String className) {
        return null;
    }

    @Override
    public Flux<DataBuffer> getBranch(RepositoryDto repositoryDto, String branch, User user) {
        String apiBaseUrl = getApiUrl(repositoryDto);

        String apiUrl = String.format("%s/archive/refs/heads/%s.zip",
                apiBaseUrl,
                branch
        );

        return webClient
                .get()
                .uri(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .headers(headers -> {
                    if (repositoryDto.getSpecification().getToken() != null && !repositoryDto.getSpecification().getToken().isEmpty()) {
                        headers.setBearerAuth(repositoryDto.getSpecification().getToken());
                    }
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        Mono.error(new CommonRuntimeException("Branch not found or unauthorized: {}", branch))
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        Mono.error(new CommonRuntimeException("GitHub server error while fetching branch: {}", branch))
                )
                .onStatus(HttpStatusCode::is3xxRedirection, response ->
                        Mono.error(new RuntimeException("Unexpected redirect from GitHub"))
                )
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class);
    }


    private String extractRepoPath(String repoUrl) {
        // https://github.com/spring-projects/spring-boot
        String[] parts = repoUrl.replace("https://github.com/", "").split("/");
        if (parts.length < 2) {
            throw new CommonRuntimeException("Invalid GitHub repository URL: {}", repoUrl);
        }
        return parts[0] + "/" + parts[1].replace(".git", "");
    }


    private String decodeContent(String content, String encoding) {
        if ("base64".equals(encoding) && content != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(content.replace("\n", ""));
            return new String(decodedBytes, StandardCharsets.UTF_8);
        }
        return content;
    }

    private String getApiUrl(RepositoryDto repositoryDto) {
        return repositoryDto.getSpecification().getBaseUrl() + "/" + repositoryDto.getPath();
    }

}
