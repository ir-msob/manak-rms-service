package ir.msob.manak.rms.gitprovider;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.rms.gitprovider.github.GithubProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GitProviderHubService {

    private final GithubProviderService githubProviderService;


    public GitProviderService getProvider(String repoUrl) {
        String provider = detectProvider(repoUrl);
        if ("github".equals(provider)) {
            return githubProviderService;
        }
        throw new CommonRuntimeException("Provider not found");
    }


    private String detectProvider(String repoUrl) {
        if (repoUrl.contains("github")) {
            return "github";
        }

        throw new CommonRuntimeException("Git provider not recognized for: " + repoUrl);
    }
}
