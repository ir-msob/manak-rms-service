package ir.msob.manak.rms.tool;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.rms.dto.FileContentBasicDto;
import ir.msob.manak.domain.model.rms.dto.FileContentDto;
import ir.msob.manak.domain.model.toolhub.ToolHandler;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.RequestSchema;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseSchema;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ToolDescriptor;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ToolParameter;
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

@Service
public class GetFileContentTool implements ToolHandler {
    private static final Logger log = LoggerFactory.getLogger(GetFileContentTool.class);

    private final GitProviderHubService gitProviderHubService;
    private final RepositoryService repositoryService;

    public GetFileContentTool(GitProviderHubService gitProviderHubService, RepositoryService repositoryService) {
        this.gitProviderHubService = gitProviderHubService;
        this.repositoryService = repositoryService;
    }

    @Override
    public ToolDescriptor getToolDescriptor() {
        // -------------------------------
        // Input ToolParameters
        // -------------------------------
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
                .build();

        RequestSchema inputSchema = RequestSchema.builder()
                .toolId(toolIdParam)
                .params(Map.of(
                        "repositoryId", repositoryIdParam,
                        "filePath", filePathParam,
                        "branch", branchParam
                ))
                .build();

        // -------------------------------
        // Output ToolParameters (FileContentDto)
        // -------------------------------
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

        // -------------------------------
        // Build ToolDescriptor
        // -------------------------------
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


    @Override
    public Mono<InvokeResponse> execute(InvokeRequest request, User user) {
        String repositoryId = (String) request.getParams().get("repositoryId");
        String filePath = (String) request.getParams().get("filePath");
        Optional<String> branchOpt = Optional.ofNullable((String) request.getParams().get("branch"));

        return repositoryService.getOne(repositoryId, user)
                .flatMap(repo -> {
                    String branch = gitProviderHubService.getBranch(repo, branchOpt.orElse(null));
                    String repoPath = gitProviderHubService.getRepositoryPath(repo);
                    String token = gitProviderHubService.getToken(repo);

                    return gitProviderHubService.getProvider(repo)
                            .downloadFile(repoPath, branch, filePath, token, user)
                            .map(this::decodeContent)
                            .map(this::cast)
                            .map(content -> InvokeResponse.builder()
                                    .toolId(request.getToolId())
                                    .res(content)
                                    .build());
                });
    }


    private FileContentDto decodeContent(FileContentDto dto) {
        if ("base64".equals(dto.getEncoding()) && dto.getContent() != null) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(dto.getContent().replace("\n", ""));
                dto.setContent(new String(decodedBytes, StandardCharsets.UTF_8));
                dto.setEncoding(null);
            } catch (IllegalArgumentException e) {
                log.error("⚠️ [GitHub] Failed to decode Base64 content: {}", e.getMessage());
                throw new CommonRuntimeException("Failed to decode Base64 content");
            }
        }
        return dto;
    }

    public FileContentBasicDto cast(FileContentDto dto) {
        if (dto == null) return null;
        return FileContentBasicDto.builder()
                .name(dto.getName())
                .path(dto.getPath())
                .size(dto.getSize())
                .content(dto.getContent())
                .build();
    }
}
