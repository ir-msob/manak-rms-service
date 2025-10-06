package ir.msob.manak.rms.repositoryspecification;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.kafka.domain.DomainCrudKafkaListenerTest;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecification;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationCriteria;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationDto;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationTypeReference;
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
public class RepositorySpecificationKafkaListenerIT
        extends DomainCrudKafkaListenerTest<RepositorySpecification, RepositorySpecificationDto, RepositorySpecificationCriteria, RepositorySpecificationRepository, RepositorySpecificationService, RepositorySpecificationDataProvider>
        implements RepositorySpecificationTypeReference {

    @SneakyThrows
    @BeforeAll
    public static void beforeAll() {
        CoreTestData.init(new ObjectId(), new ObjectId());
    }

    @SneakyThrows
    @BeforeEach
    public void beforeEach() {
        getDataProvider().cleanups();
        RepositorySpecificationDataProvider.createMandatoryNewDto();
        RepositorySpecificationDataProvider.createNewDto();
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return RepositorySpecificationKafkaListener.class;
    }

    @Override
    public String getBaseUri() {
        return RepositorySpecificationKafkaListener.BASE_URI;
    }

}
