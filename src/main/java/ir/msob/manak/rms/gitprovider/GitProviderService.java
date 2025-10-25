package ir.msob.manak.rms.gitprovider;

import ir.msob.manak.core.model.jima.security.User;
import ir.msob.manak.domain.model.rms.dto.FileContentDto;
import ir.msob.manak.domain.model.rms.repository.RepositoryDto;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GitProviderService {

    Mono<FileContentDto> getFileContent(String id, String branch, String filePath);

    Flux<FileContentDto> getMethodUsage(String id, String branch, String filePath, String method);

    Flux<FileContentDto> getClassUsage(String id, String branch, String filePath, String className);

    Flux<DataBuffer> getBranch(RepositoryDto repositoryDto, String branch, User user);
}
