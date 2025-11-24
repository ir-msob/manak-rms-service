package ir.msob.manak.rms.scm.tool;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.common.model.ParameterDescriptor;
import ir.msob.manak.domain.model.common.model.RetryPolicy;
import ir.msob.manak.domain.model.common.model.TimeoutPolicy;
import ir.msob.manak.domain.model.toolhub.ToolExecutor;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.Example;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseDescriptor;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ResponseStatus;
import ir.msob.manak.domain.model.toolhub.toolprovider.tooldescriptor.ToolDescriptor;
import ir.msob.manak.domain.model.util.VariableUtils;
import ir.msob.manak.domain.service.toolhub.util.ToolExecutorUtil;
import ir.msob.manak.rms.scm.scmprovider.ScmOperationService;
import ir.msob.manak.rms.scm.scmprovider.ScmProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static ir.msob.manak.domain.model.rms.RmsConstants.*;

/**
 * Reactive tool for fetching the content of a file from a Git repository.
 * <p>
 * This tool retrieves a file’s metadata and decoded content via {@link ScmProviderRegistry},
 * returning results in a consistent {@link InvokeResponse}.
 * <p>
 * All exceptions are caught and transformed into structured {@link InvokeResponse.ErrorInfo} responses.
 */
@Service
@RequiredArgsConstructor
public class GetFileContentTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(GetFileContentTool.class);

    private final ScmOperationService scmOperationService;


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
        String requestId = request.getRequestId();
        String toolId = request.getToolId();
        String repositoryId = VariableUtils.safeString(request.getParameters().get(REPOSITORY_ID_KEY));
        String filePath = VariableUtils.safeString(request.getParameters().get(FILE_PATH_KEY));
        String branch = Optional.ofNullable(VariableUtils.safeString(request.getParameters().get(BRANCH_KEY))).orElse("main");

        log.info("🛠️ [{}] Fetching file content: repo={}, path={}, branch={}", toolId, repositoryId, filePath, branch);

        return scmOperationService.readFile(repositoryId, branch, filePath, user)
                .map(content -> {
                    log.info("✅ [{}] Successfully fetched file '{}'", toolId, content.getPath());
                    return InvokeResponse.builder()
                            .requestId(requestId)
                            .toolId(toolId)
                            .result(content)
                            .executedAt(Instant.now())
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("❌ [{}] Error during execution", toolId, e);
                    return Mono.just(InvokeResponse.builder()
                            .requestId(requestId)
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
}