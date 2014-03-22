package eu.toolchain.exposr.repository;


import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.HandleBuilder.Handle;
import eu.toolchain.exposr.taskmanager.TaskManager;
import eu.toolchain.exposr.taskmanager.TaskSnapshot;
import eu.toolchain.exposr.tasks.BuildTask;
import eu.toolchain.exposr.tasks.DeployTask;
import eu.toolchain.exposr.tasks.SyncTask;
import eu.toolchain.exposr.tasks.SyncTask.SyncResult;

@Slf4j
public class LocalRepository implements Repository {
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
        public void done(TaskSnapshot task, SyncResult result) {
            if (result.isUpdated()) {
                build(project);
            } else {
                log.info("Not building since project has not been updated: "
                        + project);
            }

            projectReporter.reportSync(task.getId(), project, result.getId()
                    .name(), null);
        }

        @Override
        public void error(TaskSnapshot task, Throwable t) {
            log.warn("Not triggering build because sync resulted in failure: "
                    + project);

            projectReporter.reportSync(task.getId(), project, null, t);
        }
    }

    private final class BuildCallback implements Handle<Void> {
        private final Project project;

        public BuildCallback(Project project) {
            this.project = project;
        }

        @Override
        public void done(TaskSnapshot task, Void value) {
            projectReporter.reportBuild(task.getId(), project, null);
        }

        @Override
        public void error(TaskSnapshot task, Throwable t) {
            projectReporter.reportBuild(task.getId(), project, t);
        }
    }

    @Override
    public long sync(final Project project) {
        final Path buildPath = repository.resolve(project.getName());
        final SyncTask task = new SyncTask(project, buildPath);
        return taskManager.build("synchronize " + project, task)
                .callback(new SyncCallback(project)).execute();
    }

    @Override
    public List<Long> syncAll() {
        log.info("Syncronizing All Projects");

        final List<Long> ids = new ArrayList<Long>();

        for (final Project project : projectManager.getProjects()) {
            ids.add(sync(project));
        }

        return ids;
    }

    @Override
    public long build(Project project) {
        final Path buildPath = repository.resolve(project.getName());
        final BuildTask task = new BuildTask(builder, publisher, project,
                buildPath);
        return taskManager.build("build " + project, task)
                .callback(new BuildCallback(project)).execute();
    }

    @Override
    public List<Long> buildAll() {
        log.info("Building All Projects");

        final List<Long> ids = new ArrayList<Long>();

        for (final Project project : projectManager.getProjects()) {
            ids.add(build(project));
        }

        return ids;
    }

    @Override
    public long deploy(String name, String id, InputStream inputStream) {
        final Path buildPath = repository.resolve(name);
        final DeployTask task = new DeployTask(name, id, inputStream,
                buildPath, publisher);
        return taskManager.build("deploy " + name, task).execute();
    }
}