package eu.toolchain.exposr.project;

import java.util.List;

import eu.toolchain.exposr.taskmanager.HandleBuilder;

public interface ProjectManager {
    public HandleBuilder<Void> refresh();

    public List<Project> getProjects();

    public Project getProjectByName(String name);
}
