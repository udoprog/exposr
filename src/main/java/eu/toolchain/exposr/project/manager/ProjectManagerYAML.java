package eu.toolchain.exposr.project.manager;

import eu.toolchain.exposr.yaml.ValidationException;

public interface ProjectManagerYAML {
    public ProjectManager build(String context) throws ValidationException;
}