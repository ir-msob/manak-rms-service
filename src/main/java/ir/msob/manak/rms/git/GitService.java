package ir.msob.manak.rms.git;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.toolhub.dto.InvokeRequest;
import ir.msob.manak.domain.model.toolhub.dto.InvokeResponse;
import ir.msob.manak.rms.gitprovider.GitProviderHubService;
import ir.msob.manak.rms.gitprovider.GitProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GitService {
    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    private final GitProviderHubService gitProviderHubService;


    public Mono<InvokeResponse> invoke(InvokeRequest request, User user) {
        log.debug("Processing git tool: {} for user: {}", request.getToolId(), user.getId());
        String toolId = request.getToolId();

        if (toolId.equals("getFileContent")) {
            return getFileContent(request);
        }
        return Mono.error(new CommonRuntimeException("Tool not supported: " + toolId));
    }

    private Mono<InvokeResponse> getFileContent(InvokeRequest request) {
        String repoUrl = getRequiredParam(request, "repoUrl");
        String branch = getRequiredParam(request, "branch");
        String filePath = getRequiredParam(request, "filePath");

        GitProviderService provider = gitProviderHubService.getProvider(repoUrl);

        return provider.getFileContent(repoUrl, branch, filePath)
                .map(content -> buildResponse(request.getToolId(), content));

    }

    private String getRequiredParam(InvokeRequest request, String paramName) {
        return Optional.ofNullable(request.getParams().get(paramName))
                .map(Object::toString)
                .filter(value -> !value.trim().isEmpty())
                .orElseThrow(() -> new CommonRuntimeException("Parameter is required: " + paramName));
    }

    private InvokeResponse buildResponse(String toolId, Serializable result) {
        return InvokeResponse.builder()
                .toolId(toolId)
                .result(result)
                .build();
    }
}