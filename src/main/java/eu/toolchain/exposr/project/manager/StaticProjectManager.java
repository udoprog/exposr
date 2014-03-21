package eu.toolchain.exposr.project.manager;

import java.util.ArrayList;
import java.util.List;

import eu.toolchain.exposr.project.Project;

public class StaticProjectManager implements ProjectManager {
    private List<Project> projects = new ArrayList<Project>();

    public StaticProjectManager(List<Project> projects) {
        this.projects = projects;
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
