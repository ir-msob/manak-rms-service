package ir.msob.manak.rms.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.jima.core.commons.operation.BaseBeforeAfterDomainOperation;
import ir.msob.jima.crud.service.domain.BeforeAfterComponent;
import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.core.service.jima.crud.base.childdomain.ChildDomainCrudService;
import ir.msob.manak.core.service.jima.crud.base.domain.DomainCrudService;
import ir.msob.manak.core.service.jima.service.IdService;
import ir.msob.manak.domain.model.rms.dto.BranchRef;
import ir.msob.manak.domain.model.rms.dto.ScmContext;
import ir.msob.manak.domain.model.rms.repository.Repository;
import ir.msob.manak.domain.model.rms.repository.RepositoryCriteria;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import ir.msob.manak.rms.scm.scmprovider.ScmProviderRegistry;
import ir.msob.manak.rms.util.RepositoryUtil;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

@Service
public class RepositoryService
        extends DomainCrudService<Repository, RepositoryDto, RepositoryCriteria, RepositoryRepository>
        implements ChildDomainCrudService<RepositoryDto> {
    private final ScmProviderRegistry gitProviderHubService;

    private final ModelMapper modelMapper;
    private final IdService idService;
    private final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    protected RepositoryService(BeforeAfterComponent beforeAfterComponent, ObjectMapper objectMapper, RepositoryRepository repository, ModelMapper modelMapper, IdService idService, ScmProviderRegistry gitProviderHubService) {
        super(beforeAfterComponent, objectMapper, repository);
        this.modelMapper = modelMapper;
        this.idService = idService;
        this.gitProviderHubService = gitProviderHubService;
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

    @Transactional
    public Flux<DataBuffer> downloadBranch(String id, @Nullable String branch, User user) {
        log.info("🔹 Starting downloadBranch for repository id={} and branch={} by user={}", id, branch, user.getUsername());

        return getDto(id, user)
                .flatMapMany(repositoryDto -> {
                    String finalBranch = RepositoryUtil.getBranch(repositoryDto, branch);
                    String repositoryPath = RepositoryUtil.getRepositoryPath(repositoryDto);
                    String token = RepositoryUtil.getToken(repositoryDto);
                    ScmContext ctx = ScmContext.builder()
                            .repository(repositoryPath)
                            .authToken(token)
                            .build();
                    BranchRef branchRef = BranchRef.builder()
                            .name(finalBranch)
                            .build();
                    log.debug("📦 Repository info -> path={}, finalBranch={}, provider={}",
                            repositoryPath, finalBranch, repositoryDto.getSpecification().getName());

                    return gitProviderHubService.getProvider(repositoryDto)
                            .downloadArchive(ctx, branchRef)
                            .doOnSubscribe(s -> log.info("⬇️  Download started for repo={}, branch={}", repositoryPath, finalBranch))
                            .doOnNext(buffer -> log.trace("📄 Received data chunk of size={} bytes", buffer.readableByteCount()))
                            .doOnError(e -> log.error("❌ Error downloading branch {} from repo {}: {}", finalBranch, repositoryPath, e.getMessage(), e))
                            .doFinally(signal -> log.info("✅ Download finished for repo={}, branch={} [signal={}]", repositoryPath, finalBranch, signal));
                })
                .doOnError(e -> log.error("❌ Failed to initialize download for id={}, branch={}, error={}", id, branch, e.getMessage(), e))
                .doFinally(signal -> log.info("🟢 Transaction finished for downloadBranch(id={}, branch={}) [signal={}]", id, branch, signal));
    }

}
