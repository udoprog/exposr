package eu.toolchain.exposr.repository;

import java.io.InputStream;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.taskmanager.TaskSetup;
import eu.toolchain.exposr.taskmanager.SetupTaskGroup;
import eu.toolchain.exposr.tasks.SyncTask.SyncResult;

public interface Repository {
    public TaskSetup<SyncResult> sync(Project project);

    public SetupTaskGroup<SyncResult> syncAll();

    public TaskSetup<Void> build(Project project);

    public SetupTaskGroup<Void> buildAll();

    public TaskSetup<Void> deploy(String name, String id,
            InputStream inputStream);
}
