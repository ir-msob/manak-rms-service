package ir.msob.manak.rms.repositoryspecification;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.client.BaseAsyncClient;
import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.jima.crud.api.kafka.client.ChannelUtil;
import ir.msob.manak.core.service.jima.crud.kafka.domain.service.DomainCrudKafkaListener;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecification;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationCriteria;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationDto;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationTypeReference;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import static ir.msob.jima.core.commons.operation.Operations.*;

@Component
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID})
@Resource(value = RepositorySpecification.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.KAFKA)
public class RepositorySpecificationKafkaListener
        extends DomainCrudKafkaListener<RepositorySpecification, RepositorySpecificationDto, RepositorySpecificationCriteria, RepositorySpecificationRepository, RepositorySpecificationService>
        implements RepositorySpecificationTypeReference {
    public static final String BASE_URI = ChannelUtil.getBaseChannel(RepositorySpecificationDto.class);

    protected RepositorySpecificationKafkaListener(UserService userService, RepositorySpecificationService service, ObjectMapper objectMapper, ConsumerFactory<String, String> consumerFactory, BaseAsyncClient asyncClient) {
        super(userService, service, objectMapper, consumerFactory, asyncClient);
    }
}
