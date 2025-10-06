package ir.msob.manak.rms.repository;

import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.manak.core.service.jima.crud.restful.childdomain.service.CharacteristicCrudRestResource;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.rms.repository.Repository;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ir.msob.jima.core.commons.operation.Operations.*;

@RestController
@RequestMapping(RepositoryRestResource.BASE_URI)
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID, EDIT_BY_ID, GET_BY_ID, GET_PAGE})
@Resource(value = Repository.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.RESTFUL)
public class RepositoryCharacteristicRestResource extends CharacteristicCrudRestResource<RepositoryDto, RepositoryService> {

    public RepositoryCharacteristicRestResource(RepositoryService childService, UserService userService) {
        super(childService, userService);
    }
}
