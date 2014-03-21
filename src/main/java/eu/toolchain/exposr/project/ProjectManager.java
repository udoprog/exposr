package eu.toolchain.exposr.project;

import java.util.List;

import eu.toolchain.exposr.taskmanager.HandleBuilder;

public interface ProjectManager extends ProjectReporter {
    public HandleBuilder<Void> refresh();

    public List<Project> getProjects();

    public List<Project> fetchProjects() throws ProjectException;

    public Project getProjectByName(String name);
}
