package eu.toolchain.exposr.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.eclipse.jgit.lib.ObjectId;

import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.builder.ProjectBuildException;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.ExposrManifest;
import eu.toolchain.exposr.yaml.ExposrManifestYAML;

@Data
public class BuildTask implements Task<Void> {
    public static final String EXPOSR_YML = ".exposr.yml";

    private final Builder builder;
    private final Publisher publisher;
    private final Project project;
    private final Path path;

    @Override
    public Void run(TaskState state) throws Exception {
        state.system("Cleaning directory");
        project.clean(path);

        state.system("Starting build");

        final ObjectId head = project.getHead(path);

        final Path manifestPath = path.resolve(EXPOSR_YML);

        if (!Files.isRegularFile(manifestPath)) {
            throw new ProjectBuildException("Project has no manifest: "
                    + manifestPath);
        }

        final ExposrManifest manifest = ExposrManifestYAML.parse(manifestPath);

        final List<Path> paths = new ArrayList<Path>();

        for (final String publish : manifest.getPublish()) {
            paths.add(path.resolve(publish));
        }

        builder.execute(project, manifest, path, state);
        publisher.publish(project.getName(), head.name(), paths, state);
        return null;
    }
}
