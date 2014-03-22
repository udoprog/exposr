package eu.toolchain.exposr.publisher;

import java.nio.file.Path;
import java.util.List;

import eu.toolchain.exposr.taskmanager.TaskState;

public interface Publisher {
    public void publish(String name, String id, List<Path> paths,
            final TaskState state)
            throws ProjectPublishException;
}
