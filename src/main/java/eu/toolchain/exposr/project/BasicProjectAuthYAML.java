package eu.toolchain.exposr.project;

import static eu.toolchain.exposr.yaml.Utils.notEmpty;
import lombok.Getter;
import lombok.Setter;

public class BasicProjectAuthYAML implements ProjectAuthYAML {
    public static final String TYPE = "!basic-auth";

    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private String password;

    @Override
    public ProjectAuth build() {
        notEmpty("projectManager.auth.username", username);
        notEmpty("projectManager.auth.password", password);
        return new BasicProjectAuth(username, password);
    }
}