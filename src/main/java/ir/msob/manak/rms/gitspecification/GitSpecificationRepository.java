package ir.msob.manak.rms.gitspecification;

import ir.msob.jima.core.ral.mongo.commons.query.MongoQueryBuilder;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudRepository;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecification;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GitSpecificationRepository extends DomainCrudRepository<GitSpecification> {
    protected GitSpecificationRepository(MongoQueryBuilder queryBuilder, ReactiveMongoTemplate reactiveMongoTemplate) {
        super(queryBuilder, reactiveMongoTemplate);
    }
}

