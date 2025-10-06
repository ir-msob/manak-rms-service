package ir.msob.manak.rms.repository;

import ir.msob.manak.core.test.jima.crud.base.childdomain.characteristic.BaseCharacteristicCrudDataProvider;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import org.springframework.stereotype.Component;

@Component
public class RepositoryCharacteristicCrudDataProvider extends BaseCharacteristicCrudDataProvider<RepositoryDto, RepositoryService> {
    public RepositoryCharacteristicCrudDataProvider(RepositoryService childService) {
        super(childService);
    }
}
