package eu.toolchain.exposr.project;

import lombok.Getter;
import lombok.Setter;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import eu.toolchain.exposr.yaml.UtilsYAML;
import eu.toolchain.exposr.yaml.ValidationException;

public class BasicProjectAuth implements ProjectAuth {
    public static class YAML implements ProjectAuth.YAML {
        public static final String TYPE = "!basic-auth";

        @Getter
        @Setter
        private String username;

        @Getter
        @Setter
        private String password;

        @Override
        public ProjectAuth build() throws ValidationException {
            UtilsYAML.notEmpty("projectManager.auth.username", username);
            UtilsYAML.notEmpty("projectManager.auth.password", password);
            return new BasicProjectAuth(username, password);
        }
    }

    private final String username;
    private final String password;

    public BasicProjectAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void configure(GitHubClient client) {
        client.setCredentials(username, password);
    }

    @Override
    public CredentialsProvider build() {
        return new UsernamePasswordCredentialsProvider(username, password);
    }
}