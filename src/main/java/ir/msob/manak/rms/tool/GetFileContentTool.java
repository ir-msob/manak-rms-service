package ir.msob.manak.rms.tool;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.rms.dto.FileContentBasicDto;
import ir.msob.manak.domain.model.rms.dto.FileContentDto;
import ir.msob.manak.domain.model.toolhub.ToolExecutor;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.*;
import ir.msob.manak.domain.service.toolhub.util.ToolExecutorUtil;
import ir.msob.manak.rms.gitprovider.GitProviderHubService;
import ir.msob.manak.rms.repository.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Reactive tool for fetching the content of a file from a Git repository.
 * <p>
 * This tool retrieves a file’s metadata and decoded content via {@link GitProviderHubService},
 * returning results in a consistent {@link InvokeResponse}.
 * <p>
 * All exceptions are caught and transformed into structured {@link InvokeResponse.ErrorInfo} responses.
 */
@Service
public class GetFileContentTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(GetFileContentTool.class);

    private final GitProviderHubService gitProviderHubService;
    private final RepositoryService repositoryService;

    public GetFileContentTool(GitProviderHubService gitProviderHubService, RepositoryService repositoryService) {
        this.gitProviderHubService = gitProviderHubService;
        this.repositoryService = repositoryService;
    }

    @Override
    public ToolDescriptor getToolDescriptor() {
        // ==== Parameters ====
        ParameterDescriptor repositoryIdParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Repository ID to fetch the file from")
                .required(true)
                .example("123456")
                .build();

        ParameterDescriptor filePathParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("File path inside the repository")
                .required(true)
                .example("src/main/java/MyClass.java")
                .build();

        ParameterDescriptor branchParam = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.STRING)
                .description("Optional branch name (default: main)")
                .required(false)
                .defaultValue("main")
                .example("main")
                .build();

        // ==== Response Schema ====
        ParameterDescriptor responseSchema = ParameterDescriptor.builder()
                .type(ParameterDescriptor.ToolParameterType.OBJECT)
                .description("File content details")
                .property("name", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("File name")
                        .example("MyClass.java")
                        .required(true)
                        .build())
                .property("path", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("File path")
                        .example("src/main/java/MyClass.java")
                        .required(true)
                        .build())
                .property("size", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.NUMBER)
                        .description("File size in bytes")
                        .example(1024)
                        .required(true)
                        .build())
                .property("content", ParameterDescriptor.builder()
                        .type(ParameterDescriptor.ToolParameterType.STRING)
                        .description("File content")
                        .example("public class MyClass { ... }")
                        .required(true)
                        .build())
                .build();

        // ==== ToolDescriptor ====
        return ToolDescriptor.builder()
                .category("Repository")
                .name("GetFileContent")
                .displayName("Get File Content")
                .description("Fetches the content of a file from a git repository")
                .version("1.0.0")
                .tag("git")
                .tag("file")
                .parameter("repositoryId", repositoryIdParam)
                .parameter("filePath", filePathParam)
                .parameter("branch", branchParam)
                .response(ResponseDescriptor.builder()
                        .responseSchema(responseSchema)
                        .status(ResponseStatus.builder()
                                .status("SUCCESS")
                                .description("File fetched successfully")
                                .contentType("application/json")
                                .build())
                        .status(ResponseStatus.builder()
                                .status("ERROR")
                                .description("An error occurred during file fetch")
                                .contentType("application/json")
                                .build())
                        .example(Example.builder()
                                .title("Fetch file content")
                                .description("Retrieve file details from a Git repository.")
                                .input(Map.of(
                                        "repositoryId", "repo-001",
                                        "filePath", "src/Main.java",
                                        "branch", "main"))
                                .output(Map.of(
                                        "name", "Main.java",
                                        "path", "src/Main.java",
                                        "size", 2456,
                                        "content", "public class Main {...}"))
                                .build())
                        .build())
                .retryPolicy(RetryPolicy.builder()
                        .enabled(true)
                        .maxAttempts(3)
                        .initialIntervalMs(500)
                        .multiplier(2.0)
                        .maxIntervalMs(2000)
                        .build())
                .timeoutPolicy(TimeoutPolicy.builder()
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
        String filePath = (String) request.getParameters().get("filePath");
        String branch = Optional.ofNullable((String) request.getParameters().get("branch")).orElse("main");

        log.info("🛠️ [{}] Fetching file content: repo={}, path={}, branch={}", toolId, repositoryId, filePath, branch);

        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    String repoPath = gitProviderHubService.getRepositoryPath(repo);
                    String token = gitProviderHubService.getToken(repo);

                    return gitProviderHubService.getProvider(repo)
                            .downloadFile(repoPath, branch, filePath, token, user)
                            .map(this::decodeContent)
                            .map(this::cast)
                            .map(content -> {
                                log.info("✅ [{}] Successfully fetched file '{}'", toolId, content.getPath());
                                return InvokeResponse.builder()
                                        .id(requestId)
                                        .toolId(toolId)
                                        .result(content)
                                        .executedAt(Instant.now())
                                        .build();
                            });
                })
                .onErrorResume(e -> {
                    log.error("❌ [{}] Error during execution", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .id(requestId)
                            .toolId(toolId)
                            .error(InvokeResponse.ErrorInfo.builder()
                                    .code("EXECUTION_ERROR")
                                    .message(ToolExecutorUtil.buildErrorResponse(request.getToolId(), e))
                                    .stackTrace(Arrays.toString(e.getStackTrace()))
                                    .detail("repositoryId", repositoryId)
                                    .detail("filePath", filePath)
                                    .build())
                            .executedAt(Instant.now())
                            .build());
                });
    }

    private FileContentDto decodeContent(FileContentDto dto) {
        if ("base64".equalsIgnoreCase(dto.getEncoding()) && dto.getContent() != null) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(dto.getContent().replace("\n", ""));
                dto.setContent(new String(decodedBytes, StandardCharsets.UTF_8));
                dto.setEncoding(null);
                log.debug("🔍 Decoded Base64 content for file '{}'", dto.getPath());
            } catch (IllegalArgumentException e) {
                throw new CommonRuntimeException("Failed to decode Base64 content");
            }
        }
        return dto;
    }

    private FileContentBasicDto cast(FileContentDto dto) {
        if (dto == null) return null;
        return FileContentBasicDto.builder()
                .name(dto.getName())
                .path(dto.getPath())
                .size(dto.getSize())
                .content(dto.getContent())
                .build();
    }
}
