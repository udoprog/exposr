package eu.toolchain.exposr.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.lib.ObjectId;

import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.builder.ProjectBuildException;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.ExposrManifest;
import eu.toolchain.exposr.yaml.ExposrManifestYAML;

@Slf4j
public class BuildTask implements Task<Void> {
    public static final String EXPOSR_YML = ".exposr.yml";

    private final Builder builder;
    private final Publisher publisher;
    private final Project project;
    private final Path buildPath;

    public BuildTask(Builder builder,
            Publisher publisher, Project project, Path buildPath) {
        this.builder = builder;
        this.publisher = publisher;
        this.project = project;
        this.buildPath = buildPath;
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

        final ExposrManifest manifest = ExposrManifestYAML.parse(manifestFile);

        final List<Path> paths = new ArrayList<Path>();

        for (final String publish : manifest.getPublish()) {
            paths.add(buildPath.resolve(publish));
        }

        builder.execute(project, manifest, buildPath, state);
        publisher.publish(project.getName(), head.name(), paths, state);
        return null;
    }
}
