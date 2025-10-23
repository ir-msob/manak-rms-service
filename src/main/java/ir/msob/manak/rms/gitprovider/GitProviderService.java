package ir.msob.manak.rms.gitprovider;

import ir.msob.manak.domain.model.rms.dto.FileContentDto;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GitProviderService {

    Mono<FileContentDto> getFileContent(String repoUrl, String branch, String filePath);

    Flux<FileContentDto> getMethodUsage(String repoUrl, String branch, String filePath, String method);

    Flux<FileContentDto> getClassUsage(String repoUrl, String branch, String filePath, String className);

    Flux<DataBuffer> getBranch(String repoUrl, String branch);
}
