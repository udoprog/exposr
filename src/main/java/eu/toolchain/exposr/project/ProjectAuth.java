package eu.toolchain.exposr.project;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.jgit.transport.CredentialsProvider;

import eu.toolchain.exposr.yaml.ValidationException;

public interface ProjectAuth {
    public static interface YAML {
        public ProjectAuth build() throws ValidationException;
    }

    void configure(GitHubClient client);

    CredentialsProvider build();
}