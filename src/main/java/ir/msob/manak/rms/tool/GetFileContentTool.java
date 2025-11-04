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
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * {@code GetFileContentTool} is a reactive tool executor responsible for
 * fetching the content of a file from a Git-based repository.
 * <p>
 * This tool interacts with the {@link GitProviderHubService} to retrieve
 * file content, decode it if necessary (e.g. Base64-encoded), and then
 * returns the content wrapped in a {@link InvokeResponse}.
 * </p>
 * <p>
 * In case of errors (e.g., network failure, invalid parameters, or decoding issues),
 * the tool produces an {@link InvokeResponse} object with the {@code error} field populated
 * instead of throwing an exception — ensuring non-breaking reactive streams.
 * </p>
 *
 * <p><b>Example use case:</b></p>
 * <pre>
 * Request params:
 * {
 *   "repositoryId": "68fb2324f57...",
 *   "filePath": "src/main/java/com/example/Main.java",
 *   "branch": "main"
 * }
 * </pre>
 * <p>
 * The tool returns file details including name, size, and decoded content.
 * </p>
 *
 * @author
 *     Yaqub Abdi
 * @since 1.0
 */
@Service
public class GetFileContentTool implements ToolExecutor {
    private static final Logger log = LoggerFactory.getLogger(GetFileContentTool.class);

    private final GitProviderHubService gitProviderHubService;
    private final RepositoryService repositoryService;

    /**
     * Constructs a {@code GetFileContentTool} with required dependencies.
     *
     * @param gitProviderHubService the service used to interact with Git providers
     * @param repositoryService     the service for fetching repository metadata
     */
    public GetFileContentTool(GitProviderHubService gitProviderHubService, RepositoryService repositoryService) {
        this.gitProviderHubService = gitProviderHubService;
        this.repositoryService = repositoryService;
    }

    /**
     * Provides the static descriptor for this tool.
     * <p>
     * Defines input/output schemas, examples, and metadata such as version and status.
     * </p>
     *
     * @return {@link ToolDescriptor} describing this tool
     */
    @Override
    public ToolDescriptor getToolDescriptor() {
        // Input parameters
        ToolParameter toolIdParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.STRING)
                .description("The tool ID")
                .required(true)
                .example("getFileContent")
                .build();

        ToolParameter repositoryIdParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.STRING)
                .description("Repository ID to fetch the file from")
                .required(true)
                .example("123456")
                .build();

        ToolParameter filePathParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.STRING)
                .description("File path inside the repository")
                .required(true)
                .example("src/main/java/MyClass.java")
                .build();

        ToolParameter branchParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.STRING)
                .description("Optional branch name. Defaults to main if not provided")
                .required(false)
                .example("main")
                .defaultValue("main")
                .build();

        RequestSchema inputSchema = RequestSchema.builder()
                .toolId(toolIdParam)
                .params(Map.of(
                        "repositoryId", repositoryIdParam,
                        "filePath", filePathParam,
                        "branch", branchParam
                ))
                .build();

        // Output parameters
        ToolParameter nameParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.STRING)
                .description("The file name")
                .required(true)
                .example("MyClass.java")
                .build();

        ToolParameter pathParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.STRING)
                .description("The full path of the file")
                .required(true)
                .example("src/main/java/MyClass.java")
                .build();

        ToolParameter sizeParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.NUMBER)
                .description("File size in bytes")
                .required(true)
                .example(1024)
                .build();

        ToolParameter contentParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.STRING)
                .description("File content")
                .required(true)
                .example("public class MyClass { ... }")
                .build();

        ToolParameter errorParam = ToolParameter.builder()
                .type(ToolParameter.ToolParameterType.STRING)
                .description("Error message if the tool execution fails")
                .required(false)
                .example("File not found")
                .build();

        ResponseSchema outputSchema = ResponseSchema.builder()
                .toolId(toolIdParam)
                .res(ToolParameter.builder()
                        .type(ToolParameter.ToolParameterType.OBJECT)
                        .description("File content details")
                        .properties(Map.of(
                                "name", nameParam,
                                "path", pathParam,
                                "size", sizeParam,
                                "content", contentParam
                        ))
                        .required(true)
                        .build())
                .error(errorParam)
                .build();

        return ToolDescriptor.builder()
                .name("Get File Content Tool")
                .key("getFileContent")
                .description("Fetches the content of a file from a git repository")
                .version("v1")
                .inputSchema(inputSchema)
                .outputSchema(outputSchema)
                .status(ToolDescriptor.ToolDescriptorStatus.ACTIVE)
                .build();
    }

    /**
     * Executes the tool logic reactively.
     * <p>
     * This method:
     * <ul>
     *     <li>Fetches repository metadata using {@link RepositoryService}</li>
     *     <li>Determines the correct branch and token using {@link GitProviderHubService}</li>
     *     <li>Downloads file content from the provider</li>
     *     <li>Decodes content if Base64-encoded</li>
     *     <li>Builds and returns a reactive {@link Mono} of {@link InvokeResponse}</li>
     * </ul>
     * </p>
     *
     * <p>All errors are handled internally and returned as an {@link InvokeResponse#getError()}.</p>
     *
     * @param request tool invocation request
     * @param user    current authenticated user
     * @return a reactive {@link Mono} containing either the file content or an error message
     */
    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String toolId = request.getToolId();
        String repositoryId = (String) request.getParams().get("repositoryId");
        String filePath = (String) request.getParams().get("filePath");
        Optional<String> branchOpt = Optional.ofNullable((String) request.getParams().get("branch"));

        log.info("🛠️ [{}] Starting execution for repositoryId={}, filePath={}, branch={}",
                toolId, repositoryId, filePath, branchOpt.orElse("main"));

        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    String branch = gitProviderHubService.getBranch(repo, branchOpt.orElse(null));
                    String repoPath = gitProviderHubService.getRepositoryPath(repo);
                    String token = gitProviderHubService.getToken(repo);

                    log.debug("📦 [{}] Repository path: {}, Branch: {}", toolId, repoPath, branch);

                    return gitProviderHubService.getProvider(repo)
                            .downloadFile(repoPath, branch, filePath, token, user)
                            .map(this::decodeContent)
                            .map(this::cast)
                            .map(content -> {
                                log.info("✅ [{}] Successfully fetched file '{}'", toolId, content.getPath());
                                return InvokeResponse.builder()
                                        .toolId(toolId)
                                        .res(content)
                                        .build();
                            });
                })
                .onErrorResume(e -> {
                    String errorMsg = ToolExecutorUtil.buildErrorResponse(toolId, e);
                    log.error("❌ [{}] Error during execution: {}", toolId, errorMsg, e);
                    return Mono.just(InvokeResponse.builder()
                            .toolId(toolId)
                            .error(errorMsg)
                            .build());
                });
    }

    /**
     * Decodes Base64-encoded file content if needed.
     *
     * @param dto original {@link FileContentDto} containing possibly encoded content
     * @return same DTO with decoded content and null encoding field
     * @throws CommonRuntimeException if Base64 decoding fails
     */
    private FileContentDto decodeContent(FileContentDto dto) {
        if ("base64".equalsIgnoreCase(dto.getEncoding()) && dto.getContent() != null) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(dto.getContent().replace("\n", ""));
                dto.setContent(new String(decodedBytes, StandardCharsets.UTF_8));
                dto.setEncoding(null);
                log.debug("🔍 Decoded Base64 content for file '{}'", dto.getPath());
            } catch (IllegalArgumentException e) {
                log.error("⚠️ Failed to decode Base64 content for '{}': {}", dto.getPath(), e.getMessage());
                throw new CommonRuntimeException("Failed to decode Base64 content");
            }
        }
        return dto;
    }

    /**
     * Converts a {@link FileContentDto} to a simplified {@link FileContentBasicDto}.
     *
     * @param dto original file DTO
     * @return lightweight version containing essential metadata and content
     */
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
