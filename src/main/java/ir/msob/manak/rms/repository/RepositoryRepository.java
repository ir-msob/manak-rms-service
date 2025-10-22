package ir.msob.manak.rms.repository;

import ir.msob.jima.core.ral.mongo.commons.query.MongoQueryBuilder;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudRepository;
import ir.msob.manak.domain.model.rms.repository.Repository;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@org.springframework.stereotype.Repository
public class RepositoryRepository extends DomainCrudRepository<Repository> {

    protected RepositoryRepository(MongoQueryBuilder queryBuilder, ReactiveMongoTemplate reactiveMongoTemplate) {
        super(queryBuilder, reactiveMongoTemplate);
    }
}

