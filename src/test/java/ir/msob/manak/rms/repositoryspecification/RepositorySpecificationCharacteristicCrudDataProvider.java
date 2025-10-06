package ir.msob.manak.rms.repositoryspecification;

import ir.msob.manak.core.test.jima.crud.base.childdomain.characteristic.BaseCharacteristicCrudDataProvider;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationDto;
import org.springframework.stereotype.Component;

@Component
public class RepositorySpecificationCharacteristicCrudDataProvider extends BaseCharacteristicCrudDataProvider<RepositorySpecificationDto, RepositorySpecificationService> {
    public RepositorySpecificationCharacteristicCrudDataProvider(RepositorySpecificationService childService) {
        super(childService);
    }
}
