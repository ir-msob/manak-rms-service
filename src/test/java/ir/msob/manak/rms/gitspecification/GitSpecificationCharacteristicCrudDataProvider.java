package ir.msob.manak.rms.gitspecification;

import ir.msob.manak.core.test.jima.crud.base.childdomain.characteristic.BaseCharacteristicCrudDataProvider;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationDto;
import org.springframework.stereotype.Component;

@Component
public class GitSpecificationCharacteristicCrudDataProvider extends BaseCharacteristicCrudDataProvider<GitSpecificationDto, GitSpecificationService> {
    public GitSpecificationCharacteristicCrudDataProvider(GitSpecificationService childService) {
        super(childService);
    }
}
