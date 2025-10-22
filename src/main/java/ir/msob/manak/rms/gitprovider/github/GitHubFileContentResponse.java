package ir.msob.manak.rms.gitprovider.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubFileContentResponse {
    private String type;
    private String encoding;
    private Long size;
    private String name;
    private String path;
    private String content;
    private String sha;
    private String url;
    private String gitUrl;
    private String htmlUrl;
    private String downloadUrl;
}
