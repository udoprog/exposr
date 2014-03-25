package eu.toolchain.exposr.publisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.apache.commons.io.FileUtils;

import eu.toolchain.exposr.taskmanager.TaskState;

@Slf4j
@ToString(of = { "path" })
public class LocalPublisher implements Publisher {
    public static final Path DEFUALT_PATH = Paths.get("./publish");

    private final Path path;

    public LocalPublisher(Path path) {
        this.path = path;
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

    @Override
    public void publish(final String name, final String id,
            final List<Path> paths, final TaskState state)
            throws ProjectPublishException {
        final Path publishPath = buildPublishDirectory(path.resolve(".builds"),
                name, id, paths, state);

        final Path destination = path.resolve(name);
        final Path destinationTemp = path.resolve("." + name);
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
        final Path publishPath = path.resolve(name).resolve(id);

        if (Files.exists(publishPath)) {
            throw new ProjectPublishException("Publish path already exists: "
                    + publishPath);
        }

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
