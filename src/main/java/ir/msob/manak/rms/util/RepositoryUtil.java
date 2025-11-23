package ir.msob.manak.rms.util;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import ir.msob.manak.domain.model.rms.repository.branch.Branch;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.util.Strings;

public class RepositoryUtil {
    public static String getBranch(RepositoryDto repositoryDto, @Nullable String branch) {
        if (Strings.isNotBlank(branch)) {
            return branch;
        }
        return getBranch(repositoryDto);
    }

    public static String getBranch(RepositoryDto repositoryDto) {
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

    public static String getRepositoryPath(RepositoryDto repositoryDto) {
        return repositoryDto.getSpecification().getBaseUrl() + "/" + repositoryDto.getPath();
    }

    public static String getToken(RepositoryDto repositoryDto) {
        return repositoryDto.getSpecification().getToken();
    }
}
