package ir.msob.manak.rms.gitprovider.github;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.rms.dto.FileContentDto;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecification;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecificationCriteria;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecificationDto;
import ir.msob.manak.rms.gitprovider.GitProviderService;
import ir.msob.manak.rms.gitspecification.GitSpecificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class GithubProviderService implements GitProviderService {
    private final GitSpecificationService gitSpecificationService;
    private final UserService userService;
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public Mono<FileContentDto> getFileContent(String repoUrl, String branch, String filePath) {
        return getGitSpecification(repoUrl)
                .flatMap(gitSpecification -> {
                    String apiUrl = getApiUrl(gitSpecification, repoUrl, filePath);
                    return webClient
                            .get()
                            .uri(apiUrl)
                            .header("Accept", "application/vnd.github.v3+json")
                            .header("Authorization", "token " + gitSpecification.getToken())
                            .retrieve()
                            .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                                if (clientResponse.statusCode().value() == 404) {
                                    throw new CommonRuntimeException("File not found {}", filePath);
                                }
                                return Mono.error(new CommonRuntimeException("Error"));
                            })
                            .bodyToMono(GitHubFileContentResponse.class)
                            .map(response -> FileContentDto.builder()
                                    .repoUrl(repoUrl)
                                    .filePath(filePath)
                                    .branch(branch)
                                    .content(decodeContent(response.getContent(), response.getEncoding()))
                                    .build());
                });
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
    public Flux<DataBuffer> getBranch(String repoUrl, String branch) {
        return getGitSpecification(repoUrl)
                .flatMapMany(gitSpecification -> {
                    String apiBaseUrl = getApiUrl(gitSpecification, repoUrl, null);

                    String apiUrl = String.format("%s/repos/%s/zipball/%s",
                            apiBaseUrl.replaceAll("/$", ""),
                            extractRepoPath(repoUrl),
                            branch
                    );

                    return webClient
                            .get()
                            .uri(apiUrl)
                            .header("Accept", "application/vnd.github.v3+json")
                            .headers(headers -> {
                                if (gitSpecification.getToken() != null && !gitSpecification.getToken().isEmpty()) {
                                    headers.setBearerAuth(gitSpecification.getToken());
                                }
                            })
                            .retrieve()
                            .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                                    Mono.error(new CommonRuntimeException("Branch not found or unauthorized: {}", branch))
                            )
                            .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                                    Mono.error(new CommonRuntimeException("GitHub server error while fetching branch: {}", branch))
                            )
                            .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class);
                });
    }


    private String extractRepoPath(String repoUrl) {
        // مثال ورودی: https://github.com/spring-projects/spring-boot
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

    private String getApiUrl(GitSpecification gitSpecification, String repoUrl, String filePath) {
        return gitSpecification.getApiUrl();
    }

    private Mono<GitSpecificationDto> getGitSpecification(String repoUrl) {
        return gitSpecificationService.getOne(new GitSpecificationCriteria(), userService.getSystemUser());
    }


}
