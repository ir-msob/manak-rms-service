package ir.msob.manak.rms.gitspecification;

import ir.msob.jima.core.commons.resource.BaseResource;
import ir.msob.jima.core.test.CoreTestData;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.test.jima.crud.restful.domain.DomainCrudRestResourceTest;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecification;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationCriteria;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationDto;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationTypeReference;
import ir.msob.manak.rms.Application;
import ir.msob.manak.rms.ContainerConfiguration;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@AutoConfigureWebTestClient
@SpringBootTest(classes = {Application.class, ContainerConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@CommonsLog
public class GitSpecificationRestResourceIT
        extends DomainCrudRestResourceTest<GitSpecification, GitSpecificationDto, GitSpecificationCriteria, GitSpecificationRepository, GitSpecificationService, GitSpecificationDataProvider>
        implements GitSpecificationTypeReference {

    @SneakyThrows
    @BeforeAll
    public static void beforeAll() {
        CoreTestData.init(new ObjectId(), new ObjectId());
    }

    @SneakyThrows
    @BeforeEach
    public void beforeEach() {
        getDataProvider().cleanups();
        GitSpecificationDataProvider.createMandatoryNewDto();
        GitSpecificationDataProvider.createNewDto();
    }


    @Override
    public String getBaseUri() {
        return GitSpecificationRestResource.BASE_URI;
    }

    @Override
    public Class<? extends BaseResource<String, User>> getResourceClass() {
        return GitSpecificationRestResource.class;
    }

}
