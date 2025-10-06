package ir.msob.manak.rms.repository;

import ir.msob.jima.core.ral.mongo.commons.query.QueryBuilder;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudRepository;
import ir.msob.manak.domain.model.rms.repository.Repository;
import ir.msob.manak.domain.model.rms.repository.RepositoryCriteria;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@org.springframework.stereotype.Repository
public class RepositoryRepository extends DomainCrudRepository<Repository, RepositoryCriteria> {
    protected RepositoryRepository(ReactiveMongoTemplate reactiveMongoTemplate) {
        super(reactiveMongoTemplate);
    }

    @Override
    public QueryBuilder criteria(QueryBuilder query, RepositoryCriteria criteria, User user) {
        return super.criteria(query, criteria, user);
    }
}

