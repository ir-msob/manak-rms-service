package ir.msob.manak.rms.tool;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.toolhub.ToolHandler;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.RequestSchema;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseSchema;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ToolDescriptor;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ToolParameter;
import ir.msob.manak.rms.gitprovider.GitProviderHubService;
import ir.msob.manak.rms.repository.RepositoryService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Service
public class GetFileContentTool implements ToolHandler {

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
                .type("string")
                .description("The tool ID")
                .required(true)
                .example("getFileContent")
                .build();

        ToolParameter repositoryIdParam = ToolParameter.builder()
                .type("string")
                .description("Repository ID to fetch the file from")
                .required(true)
                .example("repo-123")
                .build();

        ToolParameter filePathParam = ToolParameter.builder()
                .type("string")
                .description("File path inside the repository")
                .required(true)
                .example("src/main/java/MyClass.java")
                .build();

        ToolParameter branchParam = ToolParameter.builder()
                .type("string")
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
                .type("string")
                .description("The file name")
                .required(true)
                .example("MyClass.java")
                .build();

        ToolParameter pathParam = ToolParameter.builder()
                .type("string")
                .description("The full path of the file")
                .required(true)
                .example("src/main/java/MyClass.java")
                .build();

        ToolParameter shaParam = ToolParameter.builder()
                .type("string")
                .description("SHA hash of the file content")
                .required(true)
                .example("abcd1234efgh5678")
                .build();

        ToolParameter sizeParam = ToolParameter.builder()
                .type("number")
                .description("File size in bytes")
                .required(true)
                .example(1024)
                .build();

        ToolParameter urlParam = ToolParameter.builder()
                .type("string")
                .description("Download URL of the file")
                .required(true)
                .example("https://repo.com/file/MyClass.java")
                .build();

        ToolParameter contentParam = ToolParameter.builder()
                .type("string")
                .description("File content")
                .required(true)
                .example("public class MyClass { ... }")
                .build();

        ToolParameter encodingParam = ToolParameter.builder()
                .type("string")
                .description("Encoding type (e.g., base64, utf-8)")
                .required(true)
                .example("base64")
                .build();

        ToolParameter errorParam = ToolParameter.builder()
                .type("string")
                .description("Error message if the tool execution fails")
                .required(false)
                .example("File not found")
                .build();

        ResponseSchema outputSchema = ResponseSchema.builder()
                .toolId(toolIdParam)
                .res(Map.of(
                        "name", nameParam,
                        "path", pathParam,
                        "sha", shaParam,
                        "size", sizeParam,
                        "url", urlParam,
                        "content", contentParam,
                        "encoding", encodingParam
                ))
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
                            .map(content -> InvokeResponse.builder()
                                    .toolId(request.getToolId())
                                    .res(content)
                                    .build());
                });
    }
}
