package eu.toolchain.exposr.project;

import eu.toolchain.exposr.yaml.ValidationException;

public interface ProjectAuthYAML {
    public ProjectAuth build() throws ValidationException;
}