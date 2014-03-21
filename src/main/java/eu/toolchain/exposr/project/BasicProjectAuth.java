package eu.toolchain.exposr.project;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class BasicProjectAuth implements ProjectAuth {
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