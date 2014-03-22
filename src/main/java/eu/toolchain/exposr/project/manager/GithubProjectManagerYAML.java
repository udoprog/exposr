package eu.toolchain.exposr.project.manager;

import static eu.toolchain.exposr.yaml.Utils.notEmpty;
import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.project.ProjectAuth;
import eu.toolchain.exposr.project.ProjectAuthYAML;
import eu.toolchain.exposr.yaml.ValidationException;

public class GithubProjectManagerYAML implements ProjectManagerYAML {
    public static final String TYPE = "!github-project-manager";

    @Getter
    @Setter
    private String apiUrl = "https://api.github.com";

    @Getter
    @Setter
    private String ref = "refs/heads/master";

    @Getter
    @Setter
    private String user;

    @Getter
    @Setter
    private ProjectAuthYAML auth;

    @Override
    public ProjectManager build(String context) throws ValidationException {
        notEmpty(context + ".user", user);
        final ProjectAuth auth;

        if (this.auth != null) {
            auth = this.auth.build();
        } else {
            auth = null;
        }

        return new GithubProjectManager(apiUrl, user, ref, auth);
    }
}