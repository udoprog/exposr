package eu.toolchain.exposr.publisher;

import java.nio.file.Path;
import java.util.List;

import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.ValidationException;

public interface Publisher {
    public interface YAML {
        public Publisher build(String context) throws ValidationException;
    }

    public void publish(String name, String id, List<Path> paths,
            final TaskState state)
            throws ProjectPublishException;
}
