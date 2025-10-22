package ir.msob.manak.rms.gitprovider.github;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.git.dto.FileContentDto;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecification;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationCriteria;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationDto;
import ir.msob.manak.rms.gitprovider.GitProviderService;
import ir.msob.manak.rms.gitspecification.GitSpecificationService;
import lombok.RequiredArgsConstructor;
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

    @Override
    public Flux<FileContentDto> getMethodUsage(String repoUrl, String filePath, String method) {
        return Flux.empty();
    }

    @Override
    public Flux<FileContentDto> getClassUsage(String repoUrl, String filePath, String className) {
        return Flux.empty();
    }
}
