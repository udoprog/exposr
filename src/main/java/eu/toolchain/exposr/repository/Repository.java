package eu.toolchain.exposr.repository;

import java.io.InputStream;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.taskmanager.SetupTask;
import eu.toolchain.exposr.taskmanager.SetupTaskGroup;
import eu.toolchain.exposr.tasks.SyncTaskResult;

public interface Repository {
    public SetupTask<SyncTaskResult> sync(Project project);

    public SetupTaskGroup<SyncTaskResult> syncAll();

    public SetupTask<Void> build(Project project);

    public SetupTaskGroup<Void> buildAll();

    public SetupTask<Void> deploy(String name, String id,
            InputStream inputStream);
}
