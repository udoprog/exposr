package eu.toolchain.exposr.project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.HandleBuilder.Handle;
import eu.toolchain.exposr.taskmanager.TaskManager;
import eu.toolchain.exposr.tasks.BuildTask;
import eu.toolchain.exposr.tasks.SyncTask;
import eu.toolchain.exposr.tasks.SyncTask.SyncResult;

@Slf4j
public class LocalRepository {
    @Inject
    private ProjectManager projectManager;

    @Inject
    private ProjectReporter projectReporter;

    @Inject
    private TaskManager taskManager;

    @Inject
    private Builder builder;

    @Inject
    private Publisher publisher;

    private final Path repository;

    public LocalRepository(String repository) {
        this.repository = Paths.get(repository);
    }

    private final class SyncCallback implements Handle<SyncResult> {
        private final Project project;

        public SyncCallback(Project project) {
            this.project = project;
        }

        @Override
        public void done(SyncResult result) {
            if (result.isUpdated()) {
                build(project);
            } else {
                log.info("Not building since project has not been updated: "
                        + project);
            }

            projectReporter.reportSync(project, result.getId().name(), null);
        }

        @Override
        public void error(Throwable t) {
            log.warn("Not triggering build because sync resulted in failure: "
                    + project);

            projectReporter.reportSync(project, null, t);
        }
    }

    private final class BuildCallback implements Handle<Void> {
        private final Project project;

        public BuildCallback(Project project) {
            this.project = project;
        }

        @Override
        public void done(Void value) {
            projectReporter.reportBuild(project, null);
        }

        @Override
        public void error(Throwable t) {
            projectReporter.reportBuild(project, t);
        }
    }

    public long sync(final Project project) {
        final Path buildPath = repository.resolve(project.getName());
        final SyncTask task = new SyncTask(project, buildPath);
        return taskManager.build("synchronize " + project, task)
                .callback(new SyncCallback(project)).execute();
    }

    public List<Long> syncAll() {
        log.info("Syncronizing All Projects");

        final List<Long> taskIds = new ArrayList<Long>();

        for (final Project project : projectManager.getProjects()) {
            taskIds.add(sync(project));
        }

        return taskIds;
    }

    public long build(Project project) {
        final Path buildPath = repository.resolve(project.getName());
        final BuildTask task = new BuildTask(builder,
                publisher, project, buildPath);
        return taskManager.build("build " + project, task)
                .callback(new BuildCallback(project)).execute();
    }

    public List<Long> buildAll() {
        log.info("Building All Projects");

        final List<Long> taskIds = new ArrayList<Long>();

        for (final Project project : projectManager.getProjects()) {
            taskIds.add(build(project));
        }

        return taskIds;
    }
}
