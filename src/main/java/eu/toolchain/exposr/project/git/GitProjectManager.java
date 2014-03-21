package eu.toolchain.exposr.project.git;

import java.util.ArrayList;
import java.util.List;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectManager;
import eu.toolchain.exposr.taskmanager.HandleBuilder;

public class GitProjectManager implements ProjectManager {
    private List<Project> projects = new ArrayList<Project>();

    public GitProjectManager(List<Project> projects) {
        this.projects = projects;
    }

    @Override
    public HandleBuilder<Void> refresh() {
        return null;
    }

    @Override
    public synchronized List<Project> getProjects() {
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
