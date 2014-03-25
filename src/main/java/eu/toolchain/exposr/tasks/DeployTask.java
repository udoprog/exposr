package eu.toolchain.exposr.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.ToString;

import org.apache.commons.io.FileUtils;

import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;

@ToString(of = { "name", "id", "path", "publisher" })
public class DeployTask implements Task<Void> {
    public static final int BUFFER_SIZE = 4096;

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
            FileUtils.forceMkdir(path.toFile());
        }

        state.system("Writing: " + writePath);
        writeFile(writePath);

        final List<Path> paths = new ArrayList<Path>();
        paths.add(writePath);

        publisher.publish(name, id, paths, state);
        return null;
    }

    private void writeFile(Path writePath) throws IOException {
        final File targetFile = writePath.toFile();

        int read = 0;
        byte[] buffer = new byte[BUFFER_SIZE];

        final OutputStream out = new FileOutputStream(targetFile);

        while ((read = input.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        out.flush();
        out.close();
    }
}