package ir.msob.manak.rms.gitprovider;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import ir.msob.manak.domain.model.rms.repository.branch.Branch;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecification;
import ir.msob.manak.rms.gitprovider.github.GithubProviderService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GitProviderHubService {
    private final GithubProviderService githubProviderService;
    private final Logger log = LoggerFactory.getLogger(GitProviderHubService.class);

    public GitProviderService getProvider(String type) {
        if ("github".equals(type)) {
            return githubProviderService;
        }
        throw new CommonRuntimeException("Provider not found");
    }

    public GitProviderService getProvider(RepositoryDto repositoryDto) {
        return getProvider(repositoryDto.getSpecification());
    }

    public GitProviderService getProvider(RepositorySpecification repositorySpecification) {
        return getProvider(repositorySpecification.getType());
    }

    public String getBranch(RepositoryDto repositoryDto, @Nullable String branch) {
        if (Strings.isNotBlank(branch)) {
            return branch;
        }
        return getBranch(repositoryDto);
    }

    public String getBranch(RepositoryDto repositoryDto) {
        return repositoryDto.getBranches()
                .stream()
                .filter(Branch::isDefaultBranch)
                .map(Branch::getName)
                .findFirst()
                .orElse(repositoryDto.getSpecification().getBranches().stream()
                        .filter(Branch::isDefaultBranch)
                        .map(Branch::getName)
                        .findFirst()
                        .orElseThrow(() -> new CommonRuntimeException("Branch not found"))
                );

    }

    public String getRepositoryPath(RepositoryDto repositoryDto) {
        return repositoryDto.getSpecification().getBaseUrl() + "/" + repositoryDto.getPath();
    }

    public String getToken(RepositoryDto repositoryDto) {
        return repositoryDto.getSpecification().getToken();
    }
}
