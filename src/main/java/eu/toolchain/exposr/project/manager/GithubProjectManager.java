package eu.toolchain.exposr.project.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectAuth;
import eu.toolchain.exposr.project.ProjectException;
import eu.toolchain.exposr.taskmanager.SetupTask;
import eu.toolchain.exposr.taskmanager.TaskManager;
import eu.toolchain.exposr.tasks.RefreshTask;

@Slf4j
@ToString(of = { "user", "remoteName" })
public class GithubProjectManager implements RefreshableProjectManager {
    private final GitHubClient client;
    private final String user;
    private final String remoteName;
    private final ProjectAuth auth;

    private Set<Project> projects = new HashSet<Project>();

    @Inject
    private TaskManager taskManager;

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
    public synchronized List<Project> getProjects() {
        return new ArrayList<Project>(projects);
    }

    @Override
    public SetupTask<ProjectManagerRefreshed> refresh() {
        return taskManager.build("refresh " + this, new RefreshTask(this));
    }

    @Override
    public ProjectManagerRefreshed refreshNow() throws ProjectException {
        final RepositoryService service = new RepositoryService(client);

        final List<Repository> repositories;

        try {
            repositories = service.getRepositories(user);
        } catch (IOException e) {
            throw new ProjectException("Failed to list repositories", e);
        }

        final Set<Project> newProjects = new HashSet<Project>();

        for (final Repository repository : repositories) {
            final Project newProject = new Project(repository.getName(),
                    repository.getCloneUrl(), remoteName, auth);

            newProjects.add(newProject);
        }

        synchronized (this) {
            final boolean changed = !this.projects.containsAll(newProjects);

            if (changed)
                this.projects = newProjects;

            return new ProjectManagerRefreshed(changed);
        }
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
