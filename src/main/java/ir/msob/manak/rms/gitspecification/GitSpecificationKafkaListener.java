package ir.msob.manak.rms.gitspecification;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.client.BaseAsyncClient;
import ir.msob.jima.core.commons.operation.ConditionalOnOperation;
import ir.msob.jima.core.commons.resource.Resource;
import ir.msob.jima.core.commons.shared.ResourceType;
import ir.msob.jima.crud.api.kafka.client.ChannelUtil;
import ir.msob.manak.core.service.jima.crud.kafka.domain.service.DomainCrudKafkaListener;
import ir.msob.manak.core.service.jima.security.UserService;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecification;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecificationCriteria;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecificationDto;
import ir.msob.manak.domain.model.rms.gitspecification.GitSpecificationTypeReference;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import static ir.msob.jima.core.commons.operation.Operations.*;

@Component
@ConditionalOnOperation(operations = {SAVE, UPDATE_BY_ID, DELETE_BY_ID})
@Resource(value = GitSpecification.DOMAIN_NAME_WITH_HYPHEN, type = ResourceType.KAFKA)
public class GitSpecificationKafkaListener
        extends DomainCrudKafkaListener<GitSpecification, GitSpecificationDto, GitSpecificationCriteria, GitSpecificationRepository, GitSpecificationService>
        implements GitSpecificationTypeReference {
    public static final String BASE_URI = ChannelUtil.getBaseChannel(GitSpecificationDto.class);

    protected GitSpecificationKafkaListener(UserService userService, GitSpecificationService service, ObjectMapper objectMapper, ConsumerFactory<String, String> consumerFactory, BaseAsyncClient asyncClient) {
        super(userService, service, objectMapper, consumerFactory, asyncClient);
    }

}
