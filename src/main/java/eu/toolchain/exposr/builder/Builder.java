package eu.toolchain.exposr.builder;

import java.nio.file.Path;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.ExposrManifest;

public interface Builder {
    public static interface YAML {
        public Builder build(String context);
    }

    public void execute(Project project, ExposrManifest manifest,
            Path buildPath, TaskState state) throws ProjectBuildException;
}