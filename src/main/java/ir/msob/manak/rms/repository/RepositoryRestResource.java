package ir.msob.manak.rms.repository;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import ir.msob.jima.core.commons.exception.badrequest.BadRequestResponse;
import ir.msob.jima.core.commons.methodstats.MethodStats;
import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.operation.Operations;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.scope.Scope;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.restful.domain.service.DomainCrudRestResource;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.rms.repository.Repository;
import ir.msob.manak.domain.model.rms.repository.RepositoryCriteria;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.security.Principal;

import static ir.msob.jima.core.commons.operation.Operations.*;

@RestController
@RequestMapping(RepositoryRestResource.BASE_URI)
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID, EDIT_BY_ID, GET_BY_ID, GET_PAGE})
@Resource(value = Repository.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.RESTFUL)
public class RepositoryRestResource extends DomainCrudRestResource<Repository, RepositoryDto, RepositoryCriteria, RepositoryRepository, RepositoryService> {
    public static final String BASE_URI = "/api/v1/" + Repository.DOMAIN_NAME_WITH_HYPHEN;
    Logger log = LoggerFactory.getLogger(RepositoryRestResource.class);

    protected RepositoryRestResource(UserService userService, RepositoryService service) {
        super(userService, service);
    }


    @GetMapping({"{id}/branch/{branch}/download", "{id}/download"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return a domain or null"),
            @ApiResponse(code = 400, message = "If the validation operation is incorrect throws BadRequestException otherwise nothing", response = BadRequestResponse.class)})
    @Scope(operation = Operations.SAVE)
    @MethodStats
    public ResponseEntity<Flux<DataBuffer>> downloadBranch(@PathVariable("id") String id, @PathVariable(value = "branch", required = false) String branch, Principal principal) {
        log.debug("REST request to download branch {}, id {}", branch, id);
        User user = getUser(principal);
        Flux<DataBuffer> res = this.getService().downloadBranch(id, branch, user);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+branch+".zip\"")
                .body(res);
    }

}
