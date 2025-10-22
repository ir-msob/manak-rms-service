package ir.msob.manak.rms.repositoryspecification;

import ir.msob.jima.core.ral.mongo.commons.query.MongoQueryBuilder;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudRepository;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecification;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorySpecificationRepository extends DomainCrudRepository<RepositorySpecification> {

    protected RepositorySpecificationRepository(MongoQueryBuilder queryBuilder, ReactiveMongoTemplate reactiveMongoTemplate) {
        super(queryBuilder, reactiveMongoTemplate);
    }
}

