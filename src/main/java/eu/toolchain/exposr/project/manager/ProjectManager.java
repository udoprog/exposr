package eu.toolchain.exposr.project.manager;

import java.util.List;

import eu.toolchain.exposr.project.Project;

public interface ProjectManager {
    public List<Project> getProjects();

    public Project getProjectByName(String name);
}
