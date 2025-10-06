package ir.msob.manak.rms.repositoryspecification;

import ir.msob.jima.core.ral.mongo.commons.query.QueryBuilder;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudRepository;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecification;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationCriteria;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorySpecificationRepository extends DomainCrudRepository<RepositorySpecification, RepositorySpecificationCriteria> {
    protected RepositorySpecificationRepository(ReactiveMongoTemplate reactiveMongoTemplate) {
        super(reactiveMongoTemplate);
    }

    @Override
    public QueryBuilder criteria(QueryBuilder query, RepositorySpecificationCriteria criteria, User user) {
        return super.criteria(query, criteria, user);
    }
}

