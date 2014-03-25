package eu.toolchain.exposr.project.manager;

import java.util.List;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.yaml.ValidationException;

public interface ProjectManager {
    public interface YAML {
        public ProjectManager build(String context) throws ValidationException;
    }

    public List<Project> getProjects();

    public Project getProjectByName(String name);
}
