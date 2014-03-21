package eu.toolchain.exposr.project.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectBuild;
import eu.toolchain.exposr.project.ProjectException;
import eu.toolchain.exposr.project.ProjectLog;
import eu.toolchain.exposr.project.ProjectManager;
import eu.toolchain.exposr.project.ProjectReporter;
import eu.toolchain.exposr.project.ProjectSync;
import eu.toolchain.exposr.project.git.GitProject;
import eu.toolchain.exposr.project.git.GitProject.AuthProvider;
import eu.toolchain.exposr.taskmanager.DefaultTaskManager;
import eu.toolchain.exposr.taskmanager.HandleBuilder;
import eu.toolchain.exposr.taskmanager.HandleBuilder.OnDone;
import eu.toolchain.exposr.taskmanager.Handlers;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;

@Slf4j
@ToString(of = { "user", "remoteName" })
public class GithubProjectManager implements ProjectManager, ProjectReporter {
    public static interface Auth {
        void configure(GitHubClient client);
        AuthProvider buildAuthProvider();
    }

    public static class BasicAuth implements Auth {
        private final String username;
        private final String password;

        public BasicAuth(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void configure(GitHubClient client) {
            client.setCredentials(username, password);
        }

        @Override
        public AuthProvider buildAuthProvider() {
            return new AuthProvider() {
                @Override
                public CredentialsProvider buildCredentials() {
                    return new UsernamePasswordCredentialsProvider(username,
                            password);
                }
            };
        }
    }
    
    private class RefreshTask implements Task<List<Project>> {
        @Override
        public List<Project> run(TaskState state) throws Exception {
            return GithubProjectManager.this.fetchProjects();
        }
    }

    private final GitHubClient client;
    private final String user;
    private final String remoteName;
    private final Auth auth;
    private List<Project> projects = new ArrayList<Project>();

    private final ProjectLog<ProjectSync> syncs = new ProjectLog<ProjectSync>();
    private final ProjectLog<ProjectBuild> builds = new ProjectLog<ProjectBuild>();

    @Inject
    private DefaultTaskManager taskManager;

    public GithubProjectManager(String apiUrl, String user, String remoteName,
            Auth auth) {
        this.client = GitHubClient.createClient(apiUrl);
        this.user = user;
        this.remoteName = remoteName;
        this.auth = auth;

        if (auth != null) {
            auth.configure(this.client);
        }
    }

    @Override
    public HandleBuilder<Void> refresh() {
        final RefreshTask task = new RefreshTask();
        
        final HandleBuilder<List<Project>> builder = taskManager.build(
                "refresh " + this, task);

        builder.done(new OnDone<List<Project>>() {
            @Override
            public void done(List<Project> value) {
                synchronized (projects) {
                    projects = value;
                }
            }
        });

        return Handlers.<List<Project>, Void> adapter(builder);
    }

    @Override
    public synchronized List<Project> getProjects() {
        return projects;
    }

    public List<Project> fetchProjects() throws ProjectException {
        log.info("Fetching Projects");

        final RepositoryService service = new RepositoryService(client);

        final List<Repository> repositories;

        try {
            repositories = service.getRepositories(user);
        } catch (IOException e) {
            throw new ProjectException("Failed to list repositories", e);
        }

        final List<Project> projects = new ArrayList<Project>();

        final AuthProvider authProvider;

        if (auth != null) {
            authProvider = auth.buildAuthProvider();
        } else {
            authProvider = null;
        }

        for (final Repository repository : repositories) {
            projects.add(new GitProject(repository.getName(), repository
                    .getCloneUrl(), remoteName, authProvider));
        }

        return projects;
    }

    @Override
    public Project getProjectByName(String name) {
        final List<Project> projects = getProjects();

        for (Project project : projects) {
            if (project.getName().equals(name)) {
                return project;
            }
        }

        return null;
    }

    @Override
    public void reportSync(Project project, String id, Throwable error) {
        syncs.append(project, new ProjectSync(new Date(), id, error));
    }

    @Override
    public ProjectSync getLastSync(Project project) {
        return syncs.getLast(project);
    }

    @Override
    public List<ProjectSync> getSyncs(Project project) {
        return syncs.all(project);
    }

    @Override
    public void reportBuild(Project project, Throwable error) {
        builds.append(project, new ProjectBuild(new Date(), error));
    }

    @Override
    public ProjectBuild getLastBuild(Project project) {
        return builds.getLast(project);
    }

    @Override
    public List<ProjectBuild> getBuilds(Project project) {
        return builds.all(project);
    }
}
