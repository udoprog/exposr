package eu.toolchain.exposr.project.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import eu.toolchain.exposr.project.ProjectAuth;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectException;
import eu.toolchain.exposr.taskmanager.DefaultTaskManager;
import eu.toolchain.exposr.taskmanager.HandleBuilder;
import eu.toolchain.exposr.taskmanager.HandleBuilder.OnDone;
import eu.toolchain.exposr.taskmanager.Handlers;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;

@Slf4j
@ToString(of = { "user", "remoteName" })
public class GithubProjectManager implements RefreshableProjectManager {
    private class RefreshTask implements Task<List<Project>> {
        @Override
        public List<Project> run(TaskState state) throws Exception {
            return GithubProjectManager.this.fetchProjects();
        }
    }

    private final GitHubClient client;
    private final String user;
    private final String remoteName;
    private final ProjectAuth auth;
    private List<Project> projects = new ArrayList<Project>();

    @Inject
    private DefaultTaskManager taskManager;

    public GithubProjectManager(String apiUrl, String user, String remoteName,
            ProjectAuth auth) {
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

        for (final Repository repository : repositories) {
            projects.add(new Project(repository.getName(), repository
                    .getCloneUrl(), remoteName, auth));
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
}
