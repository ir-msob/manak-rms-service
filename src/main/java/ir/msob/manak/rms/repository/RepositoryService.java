package ir.msob.manak.rms.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.jima.core.commons.operation.BaseBeforeAfterDomainOperation;
import ir.msob.jima.crud.service.domain.BeforeAfterComponent;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.childdomain.ChildDomainCrudService;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.rms.repository.Repository;
import ir.msob.manak.domain.model.rms.repository.RepositoryCriteria;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

@Service
public class RepositoryService extends DomainCrudService<Repository, RepositoryDto, RepositoryCriteria, RepositoryRepository>
        implements ChildDomainCrudService<RepositoryDto> {

    private final ModelMapper modelMapper;
    private final IdService idService;

    protected RepositoryService(BeforeAfterComponent beforeAfterComponent, ObjectMapper objectMapper, RepositoryRepository repository, ModelMapper modelMapper, IdService idService) {
        super(beforeAfterComponent, objectMapper, repository);
        this.modelMapper = modelMapper;
        this.idService = idService;
    }

    @Override
    public RepositoryDto toDto(Repository domain, User user) {
        return modelMapper.map(domain, RepositoryDto.class);
    }

    @Override
    public Repository toDomain(RepositoryDto dto, User user) {
        return dto;
    }

    @Override
    public Collection<BaseBeforeAfterDomainOperation<String, User, RepositoryDto, RepositoryCriteria>> getBeforeAfterDomainOperations() {
        return Collections.emptyList();
    }

    @Transactional
    @Override
    public Mono<RepositoryDto> getDto(String id, User user) {
        return super.getOne(id, user);
    }

    @Transactional
    @Override
    public Mono<RepositoryDto> updateDto(String id, @Valid RepositoryDto dto, User user) {
        return super.update(id, dto, user);
    }

    @Override
    public BaseIdService getIdService() {
        return idService;
    }
}
