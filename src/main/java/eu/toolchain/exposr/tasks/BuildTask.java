package eu.toolchain.exposr.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.yaml.snakeyaml.Yaml;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectBuildException;
import eu.toolchain.exposr.project.ProjectException;
import eu.toolchain.exposr.project.ProjectManager;
import eu.toolchain.exposr.taskmanager.StreamReader;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.ExposrYAML;

@Slf4j
public class BuildTask implements Task<Void> {
    public static final String EXPOSR_YML = ".exposr.yml";

    private final ProjectManager projectManager;
    private final Project project;
    private final Path buildPath;
    private final Path deployPath;
    private static final ThreadLocal<Yaml> yamls = new ThreadLocal<Yaml>();

    public BuildTask(ProjectManager projectManager, Project project,
            Path buildPath, Path deployPath) {
        this.projectManager = projectManager;
        this.project = project;
        this.buildPath = buildPath;
        this.deployPath = deployPath;
    }

    private Yaml getYaml() {
        Yaml yaml = yamls.get();

        if (yaml != null) {
            return yaml;
        }

        synchronized (yamls) {
            yaml = yamls.get();

            if (yaml != null) {
                return yaml;
            }

            yaml = new Yaml();
            yamls.set(yaml);
        }

        return yaml;
    }

    @Override
    public Void run(TaskState state) throws Exception {
        log.info("Building project: " + project);

        try {
            buildProject(state);
            projectManager.reportBuild(project, null);
        } catch (ProjectException e) {
            log.error("Failed to build project", e);
            projectManager.reportBuild(project, e);
            throw e;
        }

        return null;
    }

    public void buildProject(TaskState state) throws ProjectException {
        final ObjectId head = project.getHead(buildPath);
        final Path publishPath = deployPath.resolve(".builds")
                .resolve(project.getName()).resolve(head.name());
        final Path destination = deployPath.resolve(project.getName());
        final Path destinationTemp = deployPath
                .resolve("." + project.getName());
        final Path linkPath = destination.getParent().relativize(publishPath);

        final Path manifestFile = buildPath.resolve(EXPOSR_YML);

        if (!Files.isRegularFile(manifestFile)) {
            throw new ProjectBuildException("Project has no '" + EXPOSR_YML
                    + "' manifest");
        }

        if (Files.isSymbolicLink(destinationTemp)) {
            log.info("Cleaning up dangling symlink: " + destinationTemp);

            try {
                Files.delete(destinationTemp);
            } catch (IOException e) {
                throw new ProjectBuildException("Failed to delete symlink: "
                        + destinationTemp);
            }
        }

        if (Files.exists(publishPath)) {
            throw new ProjectBuildException("Publish path already exists: "
                    + publishPath);
        }

        try {
            FileUtils.forceMkdir(publishPath.toFile());
        } catch (IOException e) {
            throw new ProjectBuildException("Failed to create directory: "
                    + publishPath, e);
        }

        final ExposrYAML manifest = parseManifest(manifestFile);

        runCommands(manifest, state);
        runPublish(manifest, publishPath);
        atomicallySymlink(destination, destinationTemp, linkPath);
    }

    private void atomicallySymlink(final Path destination,
            final Path destinationTemp, final Path linkPath)
            throws ProjectException, ProjectBuildException {
        try {
            Files.createSymbolicLink(destinationTemp, linkPath);
        } catch (IOException e) {
            throw new ProjectException("Failed to create symbolic link: "
                    + destination, e);
        }

        try {
            Files.move(destinationTemp, destination,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ProjectBuildException(
                    "Failed to move temporary symlink to: " + destination, e);
        }
    }

    private void runPublish(final ExposrYAML manifest, final Path publishPath)
            throws ProjectBuildException {
        for (final String publish : manifest.getPublish()) {
            final Path sourcePath = buildPath.resolve(publish);

            if (Files.isDirectory(sourcePath)) {
                log.info("Copying {} -> {}", sourcePath, publishPath);

                try {
                    FileUtils.copyDirectory(sourcePath.toFile(),
                            publishPath.toFile());
                } catch (IOException e) {
                    throw new ProjectBuildException("Failed to publish: "
                            + sourcePath, e);
                }

                continue;
            }

            throw new ProjectBuildException("publish: Not a directory: "
                    + sourcePath);
        }
    }

    private void runCommands(final ExposrYAML manifest, final TaskState state)
            throws ProjectBuildException {
        for (final String command : manifest.getCommands()) {
            log.info(project + ": execute: " + command);

            final String[] parts = command.split(" ");

            final ProcessBuilder builder = new ProcessBuilder()
                    .redirectOutput(Redirect.PIPE)
                    .redirectError(Redirect.PIPE)
                    .command(parts).directory(buildPath.toFile());

            final Process p;

            try {
                p = builder.start();
            } catch (IOException e) {
                throw new ProjectBuildException("Failed to run command: "
                        + command, e);
            }

            final StreamReader stdout = new StreamReader(p.getInputStream(),
                    new StreamReader.Handle() {
                        @Override
                        public void line(String line) {
                            state.error(line);
                        }
                    });

            final StreamReader stderr = new StreamReader(p.getErrorStream(),
                    new StreamReader.Handle() {
                        @Override
                        public void line(String line) {
                            state.error(line);
                        }
                    });

            stdout.start();
            stderr.start();

            final int status;

            try {
                status = p.waitFor();
            } catch (InterruptedException e) {
                throw new ProjectBuildException("Command interrupted: "
                        + command, e);
            }

            try {
                stdout.join();
            } catch (InterruptedException e) {
                log.error("stdout consumer join failed", e);
            }

            try {
                stderr.join();
            } catch (InterruptedException e) {
                log.error("stderr consumer join failed", e);
            }

            if (status != 0) {
                throw new ProjectBuildException(
                        "Command exited with non-zero exit status [" + status
                                + "]: " + command);
            }
        }
    }

    private ExposrYAML parseManifest(final Path manifestFile)
            throws ProjectBuildException {
        final ExposrYAML manifest;

        final Yaml yaml = getYaml();

        final InputStream inputStream;

        try {
            inputStream = Files.newInputStream(manifestFile);
        } catch (IOException e) {
            throw new ProjectBuildException("Failed to open manifest", e);
        }

        try {
            manifest = yaml.loadAs(inputStream, ExposrYAML.class);
        } catch (Throwable t) {
            throw new ProjectBuildException(
                    "Invalid manifest: " + manifestFile, t);
        }

        if (manifest.getPublish() == null || manifest.getPublish().isEmpty()) {
            throw new ProjectBuildException("No 'publish' declaration in "
                    + manifestFile);
        }

        return manifest;
    }
}
