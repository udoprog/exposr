package eu.toolchain.exposr.publisher;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.apache.commons.io.FileUtils;

import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.UtilsYAML;
import eu.toolchain.exposr.yaml.ValidationException;

@Slf4j
@ToString(of = { "path" })
public class LocalPublisher implements Publisher {
    public static final Path DEFUALT_PATH = Paths.get("./publish");

    public static class YAML implements Publisher.YAML {
        public static final String TYPE = "!local-publisher";

        @Getter
        @Setter
        private String path;

        @Getter
        @Setter
        private String pattern;

        @Override
        public Publisher build(String context) throws ValidationException {
            final Path path = UtilsYAML.toDirectory(context + ".path",
                    this.path, LocalPublisher.DEFUALT_PATH);
            return new LocalPublisher(path, pattern);
        }
    }

    private final Path path;
    private final String pattern;

    public LocalPublisher(Path path, String pattern) {
        this.path = path;
        this.pattern = pattern;
    }

    private void atomicallySymlink(final Path destination,
            final Path destinationTemp, final Path linkPath)
            throws ProjectPublishException {
        try {
            Files.createSymbolicLink(destinationTemp, linkPath);
        } catch (IOException e) {
            throw new ProjectPublishException(
                    "Failed to create symbolic link: " + destination, e);
        }

        try {
            Files.move(destinationTemp, destination,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ProjectPublishException(
                    "Failed to move temporary symlink to: " + destination, e);
        }
    }

    private String resolveName(String name) {
        if (pattern == null || pattern.isEmpty())
            return name;

        return String.format(pattern, name);
    }

    @Override
    public void publish(final String name, final String id,
            final List<Path> paths, final TaskState state)
            throws ProjectPublishException {
        final Path publishPath = buildPublishDirectory(path.resolve(".builds"),
                name, id, paths, state);

        final String publishName = resolveName(name);

        final Path destination = path.resolve(publishName);
        final Path destinationTemp = path.resolve("." + publishName);
        final Path linkPath = destination.getParent().relativize(publishPath);

        if (Files.isSymbolicLink(destinationTemp)) {
            log.info("Cleaning up dangling symlink: " + destinationTemp);

            try {
                Files.delete(destinationTemp);
            } catch (IOException e) {
                throw new ProjectPublishException("Failed to delete symlink: "
                        + destinationTemp);
            }
        }

        state.system("Creating symlink " + destination + " to " + linkPath);
        atomicallySymlink(destination, destinationTemp, linkPath);
    }

    public static Path buildPublishDirectory(final Path path,
            final String name, final String id, final List<Path> paths,
            final TaskState state) throws ProjectPublishException {
        final Path publishPath = makePublishPath(path, name, id, state);

        state.system("Publishing to " + publishPath);

        try {
            FileUtils.forceMkdir(publishPath.toFile());
        } catch (IOException e) {
            throw new ProjectPublishException("Failed to create directory: "
                    + publishPath, e);
        }

        for (final Path source : paths) {
            if (Files.isDirectory(source)) {
                state.system("Copying " + name + ":" + id + " to "
                        + publishPath);

                try {
                    FileUtils.copyDirectory(source.toFile(),
                            publishPath.toFile());
                } catch (IOException e) {
                    throw new ProjectPublishException("Failed to publish: "
                            + source, e);
                }

                continue;
            }

            final String baseName = source.getFileName().toString();

            if (Files.isRegularFile(source) && baseName.endsWith(".zip")) {
                state.system("Extracting " + name + ":" + id + " to "
                        + publishPath);

                try {
                    extractZipFile(source, publishPath);
                } catch (ZipException e) {
                    throw new ProjectPublishException(
                            "Failed to extract zip file: " + source);
                }

                continue;
            }

            throw new ProjectPublishException(
                    "publish: Don't know how to handle: " + source);
        }

        return publishPath;
    }

    private static Path makePublishPath(final Path path, final String name,
            final String id, TaskState state) throws ProjectPublishException {
        Path candidate = path.resolve(name).resolve(id);

        int i = 0;

        while (true) {
            try {
                Files.createDirectory(candidate);
            } catch (FileAlreadyExistsException e) {
                state.system("Rejecting candidate (exists): " + candidate);
                candidate = path.resolve(name).resolve(id + "-" + i++);
                continue;
            } catch (IOException e) {
                throw new ProjectPublishException(
                        "Failed to create publish directory",
                        e);
            }

            break;
        }

        return candidate.normalize();
    }

    public static void buildZipFile(final Path sourcePath, final Path targetPath)
            throws ZipException {
        final ZipFile zipFile = new ZipFile(targetPath.toFile());
        final ZipParameters parameters = new ZipParameters();
        zipFile.addFolder(sourcePath.toFile(), parameters);
    }

    public static void extractZipFile(final Path sourcePath,
            final Path publishPath) throws ZipException {
        final ZipFile zipFile = new ZipFile(sourcePath.toFile());
        zipFile.extractAll(publishPath.toString());
    }
}
