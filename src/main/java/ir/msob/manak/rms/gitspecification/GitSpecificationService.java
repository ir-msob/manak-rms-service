package ir.msob.manak.rms.gitspecification;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.jima.core.commons.operation.BaseBeforeAfterDomainOperation;
import ir.msob.jima.crud.service.domain.BeforeAfterComponent;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.childdomain.ChildDomainCrudService;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecification;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationCriteria;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationDto;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

@Service
public class GitSpecificationService extends DomainCrudService<GitSpecification, GitSpecificationDto, GitSpecificationCriteria, GitSpecificationRepository>
        implements ChildDomainCrudService<GitSpecificationDto> {

    private final ModelMapper modelMapper;
    private final IdService idService;

    protected GitSpecificationService(BeforeAfterComponent beforeAfterComponent, ObjectMapper objectMapper, GitSpecificationRepository repository, ModelMapper modelMapper, IdService idService) {
        super(beforeAfterComponent, objectMapper, repository);
        this.modelMapper = modelMapper;
        this.idService = idService;
    }

    @Override
    public GitSpecificationDto toDto(GitSpecification domain, User user) {
        return modelMapper.map(domain, GitSpecificationDto.class);
    }

    @Override
    public GitSpecification toDomain(GitSpecificationDto dto, User user) {
        return dto;
    }

    @Override
    public Collection<BaseBeforeAfterDomainOperation<String, User, GitSpecificationDto, GitSpecificationCriteria>> getBeforeAfterDomainOperations() {
        return Collections.emptyList();
    }

    @Transactional
    @Override
    public Mono<GitSpecificationDto> getDto(String id, User user) {
        return super.getOne(id, user);
    }

    @Transactional
    @Override
    public Mono<GitSpecificationDto> updateDto(String id, @Valid GitSpecificationDto dto, User user) {
        return super.update(id, dto, user);
    }

    @Override
    public BaseIdService getIdService() {
        return idService;
    }
}
