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

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;

import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;

@Slf4j
public class DeployTask implements Task<Void> {
    public static final int BUFFER_SIZE = 4096;

    private final String name;
    private final String id;
    private final InputStream inputStream;
    private final Path buildPath;
    private final Publisher publisher;

    public DeployTask(String name, String id, InputStream inputStream,
            Path buildPath, Publisher publisher) {
        this.name = name;
        this.inputStream = inputStream;
        this.id = id;
        this.buildPath = buildPath;
        this.publisher = publisher;
    }

    @Override
    public Void run(TaskState state) throws Exception {
        final Path writePath = buildPath.resolve(id + ".zip");

        if (!Files.isDirectory(buildPath)) {
            state.system("Making Build Directory: " + buildPath);
            FileUtils.forceMkdir(buildPath.toFile());
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

        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        out.flush();
        out.close();
    }
}