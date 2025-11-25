package ir.msob.manak.rms.gitspecification;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.kafka.domain.DomainCrudKafkaListenerTest;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecification;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecificationCriteria;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecificationDto;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecificationTypeReference;
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
class GitSpecificationKafkaListenerIT
        extends DomainCrudKafkaListenerTest<GitSpecification, GitSpecificationDto, GitSpecificationCriteria, GitSpecificationRepository, GitSpecificationService, GitSpecificationDataProvider>
        implements GitSpecificationTypeReference {

    @SneakyThrows
    @BeforeAll
    static void beforeAll() {
        CoreTestData.init(new ObjectId(), new ObjectId());
    }

    @SneakyThrows
    @BeforeEach
    void beforeEach() {
        getDataProvider().cleanups();
        GitSpecificationDataProvider.createMandatoryNewDto();
        GitSpecificationDataProvider.createNewDto();
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return GitSpecificationKafkaListener.class;
    }

    @Override
    public String getBaseUri() {
        return GitSpecificationKafkaListener.BASE_URI;
    }
}
