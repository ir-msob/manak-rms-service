package ir.msob.manak.rms.scm.tool;

import ir.msob.manak.domain.model.toolhub.ToolProviderDescriptor;
import ir.msob.manak.domain.model.toolhub.toolprovider.ToolProviderDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RmsProviderHandler implements ToolProviderDescriptor {

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public ToolProviderDto getToolProvider() {
        return ToolProviderDto.builder()
                .name(appName)
                .description("ToolProvider for application: " + appName)
                .serviceName(appName)
                .endpoint("/api/v1/tool/invoke")
                .build();
    }
}
