package eu.toolchain.exposr.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.lib.ObjectId;
import org.yaml.snakeyaml.Yaml;

import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.builder.ProjectBuildException;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.ExposrYAML;

@Slf4j
public class BuildTask implements Task<Void> {
    public static final String EXPOSR_YML = ".exposr.yml";

    private final Builder builder;
    private final Publisher publisher;
    private final Project project;
    private final Path buildPath;
    private static final ThreadLocal<Yaml> yamls = new ThreadLocal<Yaml>();

    public BuildTask(Builder builder,
            Publisher publisher, Project project, Path buildPath) {
        this.builder = builder;
        this.publisher = publisher;
        this.project = project;
        this.buildPath = buildPath;
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
        state.system("Building project in " + buildPath);

        final ObjectId head = project.getHead(buildPath);

        final Path manifestFile = buildPath.resolve(EXPOSR_YML);

        if (!Files.isRegularFile(manifestFile)) {
            throw new ProjectBuildException("Project has no '" + EXPOSR_YML
                    + "' manifest");
        }

        final ExposrYAML manifest = parseManifest(manifestFile);

        final List<Path> paths = new ArrayList<Path>();

        for (final String publish : manifest.getPublish()) {
            paths.add(buildPath.resolve(publish));
        }

        builder.execute(project, manifest, buildPath, state);
        publisher.publish(project, paths, head.name(), state);
        return null;
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
