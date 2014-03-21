package eu.toolchain.exposr.publisher;

import java.nio.file.Path;
import java.util.List;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectPublishException;
import eu.toolchain.exposr.taskmanager.TaskState;

public interface Publisher {
    public void publish(final Project project, List<Path> paths,
            final String id, final TaskState state)
            throws ProjectPublishException;
}
