package eu.toolchain.exposr.builder;

import java.nio.file.Path;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectBuildException;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.ExposrYAML;

public interface Builder {
    public void execute(Project project, ExposrYAML manifest, Path buildPath,
            TaskState state) throws ProjectBuildException;
}