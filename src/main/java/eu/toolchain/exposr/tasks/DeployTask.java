package eu.toolchain.exposr.tasks;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.ToString;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.utils.PathUtils;

@ToString(exclude={"input"})
public class DeployTask implements Task<Void> {
    private final String name;
    private final String id;
    private final Path path;
    private final Publisher publisher;
    private final InputStream input;

    public DeployTask(String name, String id, InputStream input,
            Path path, Publisher publisher) {
        this.name = name;
        this.id = id;
        this.path = path.toAbsolutePath().normalize();
        this.publisher = publisher;
        this.input = input;
    }

    @Override
    public Void run(TaskState state) throws Exception {
        final Path writePath = path.resolve(id + ".zip");

        if (!Files.isDirectory(path)) {
            state.system("Making Build Directory: " + path);
            PathUtils.forceMkdir(path);
        }

        state.system("Writing: " + writePath);
        PathUtils.writeTo(writePath, input);

        final List<Path> paths = new ArrayList<Path>();
        paths.add(writePath);

        publisher.publish(name, id, paths, state);
        return null;
    }
}