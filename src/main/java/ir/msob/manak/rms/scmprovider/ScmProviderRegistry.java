package ir.msob.manak.rms.scmprovider;

import ir.msob.jima.core.commons.exception.runtime.CommonRuntimeException;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScmProviderRegistry {

    private final Map<String, ScmProviderService> scmProviderServiceMap;

    public ScmProviderService getProvider(String type) {
        if ("github".equals(type)) {
            return scmProviderServiceMap.get("githubProviderService");
        }
        throw new CommonRuntimeException("Provider not found");
    }

    public ScmProviderService getProvider(RepositoryDto repositoryDto) {
        return getProvider(repositoryDto.getSpecification());
    }

    public ScmProviderService getProvider(RepositorySpecification repositorySpecification) {
        return getProvider(repositorySpecification.getType());
    }

}