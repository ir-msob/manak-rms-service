package ir.msob.manak.rms.repository;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.kafka.domain.DomainCrudKafkaListenerTest;
import ir.msob.manak.domain.model.rms.repository.Repository;
import ir.msob.manak.domain.model.rms.repository.RepositoryCriteria;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import ir.msob.manak.domain.model.rms.repository.RepositoryTypeReference;
import ir.msob.manak.rms.Application;
import ir.msob.manak.rms.ContainerConfiguration;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = {Application.class, ContainerConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@CommonsLog
public class RepositoryKafkaListenerIT
        extends DomainCrudKafkaListenerTest<Repository, RepositoryDto, RepositoryCriteria, RepositoryRepository, RepositoryService, RepositoryDataProvider>
        implements RepositoryTypeReference {

    @SneakyThrows
    @BeforeAll
    public static void beforeAll() {
        CoreTestData.init(new ObjectId(), new ObjectId());
    }

    @SneakyThrows
    @BeforeEach
    public void beforeEach() {
        getDataProvider().cleanups();
        RepositoryDataProvider.createMandatoryNewDto();
        RepositoryDataProvider.createNewDto();
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return RepositoryKafkaListener.class;
    }

    @Override
    public String getBaseUri() {
        return RepositoryKafkaListener.BASE_URI;
    }

}
