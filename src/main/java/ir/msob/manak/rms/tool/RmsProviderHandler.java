package ir.msob.manak.rms.tool;

import ir.msob.manak.domain.model.toolhub.ToolProviderHandler;
import ir.msob.manak.domain.model.toolhub.toolprovider.ToolProviderDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RmsProviderHandler implements ToolProviderHandler {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    @Override
    public ToolProviderDto getToolProvider() {
        return ToolProviderDto.builder()
                .name(appName)
                .description("ToolProvider for application: " + appName)
                .baseUrl(baseUrl)
                .endpoint("/api/v1/tool/invoke")
                .build();
    }
}
