package eu.toolchain.exposr.project;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.jgit.transport.CredentialsProvider;

public interface ProjectAuth {
    void configure(GitHubClient client);

    CredentialsProvider build();
}