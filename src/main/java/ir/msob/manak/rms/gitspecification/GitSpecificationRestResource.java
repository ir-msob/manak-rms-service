package ir.msob.manak.rms.gitspecification;

import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.manak.core.service.jima.crud.restful.domain.service.DomainCrudRestResource;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecification;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationCriteria;
import ir.msob.manak.domain.model.git.gitspecification.GitSpecificationDto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ir.msob.jima.core.commons.operation.Operations.*;

@RestController
@RequestMapping(GitSpecificationRestResource.BASE_URI)
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID, EDIT_BY_ID, GET_BY_ID, GET_PAGE})
@Resource(value = GitSpecification.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.RESTFUL)
public class GitSpecificationRestResource extends DomainCrudRestResource<GitSpecification, GitSpecificationDto, GitSpecificationCriteria, GitSpecificationRepository, GitSpecificationService> {
    public static final String BASE_URI = "/api/v1/" +GitSpecification.DOMAIN_NAME_WITH_HYPHEN;

    protected GitSpecificationRestResource(UserService userService, GitSpecificationService service) {
        super(userService, service);
    }
}
