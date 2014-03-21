package eu.toolchain.exposr.publisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.taskmanager.TaskState;

@Slf4j
public class LocalPublisher implements Publisher {
    private final Path deployPath;
    
    public LocalPublisher(String deployPath) {
        this.deployPath = Paths.get(deployPath);
    }
    
    private void atomicallySymlink(final Path destination,
            final Path destinationTemp, final Path linkPath)
            throws ProjectPublishException {
        try {
            Files.createSymbolicLink(destinationTemp, linkPath);
        } catch (IOException e) {
            throw new ProjectPublishException(
                    "Failed to create symbolic link: "
                    + destination, e);
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
    public void publish(final Project project, final List<Path> paths,
            final String id, final TaskState state)
            throws ProjectPublishException {
        final Path publishPath = deployPath.resolve(".builds")
                .resolve(project.getName()).resolve(id);
        final Path destination = deployPath.resolve(project.getName());
        final Path destinationTemp = deployPath
                .resolve("." + project.getName());
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

        for (final Path sourcePath : paths) {
            if (Files.isDirectory(sourcePath)) {
                log.info("Copying {} -> {}", sourcePath, publishPath);

                try {
                    FileUtils.copyDirectory(sourcePath.toFile(),
                            publishPath.toFile());
                } catch (IOException e) {
                    throw new ProjectPublishException("Failed to publish: "
                            + sourcePath, e);
                }

                continue;
            }

            throw new ProjectPublishException("publish: Not a directory: "
                    + sourcePath);
        }

        atomicallySymlink(destination, destinationTemp, linkPath);
    }
}
