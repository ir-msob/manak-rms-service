package ir.msob.manak.rms.repositoryspecification;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.jima.core.commons.operation.BaseBeforeAfterDomainOperation;
import ir.msob.jima.crud.service.domain.BeforeAfterComponent;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.childdomain.ChildDomainCrudService;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecification;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationCriteria;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationDto;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

@Service
public class RepositorySpecificationService extends DomainCrudService<RepositorySpecification, RepositorySpecificationDto, RepositorySpecificationCriteria, RepositorySpecificationRepository>
        implements ChildDomainCrudService<RepositorySpecificationDto> {

    private final ModelMapper modelMapper;
    private final IdService idService;

    protected RepositorySpecificationService(BeforeAfterComponent beforeAfterComponent, ObjectMapper objectMapper, RepositorySpecificationRepository repository, ModelMapper modelMapper, IdService idService) {
        super(beforeAfterComponent, objectMapper, repository);
        this.modelMapper = modelMapper;
        this.idService = idService;
    }

    @Override
    public RepositorySpecificationDto toDto(RepositorySpecification domain, User user) {
        return modelMapper.map(domain, RepositorySpecificationDto.class);
    }

    @Override
    public RepositorySpecification toDomain(RepositorySpecificationDto dto, User user) {
        return dto;
    }

    @Override
    public Collection<BaseBeforeAfterDomainOperation<String, User, RepositorySpecificationDto, RepositorySpecificationCriteria>> getBeforeAfterDomainOperations() {
        return Collections.emptyList();
    }

    @Transactional
    @Override
    public Mono<RepositorySpecificationDto> getDto(String id, User user) {
        return super.getOne(id, user);
    }

    @Transactional
    @Override
    public Mono<RepositorySpecificationDto> updateDto(String id, @Valid RepositorySpecificationDto dto, User user) {
        return super.update(id, dto, user);
    }

    @Override
    public BaseIdService getIdService() {
        return idService;
    }
}
