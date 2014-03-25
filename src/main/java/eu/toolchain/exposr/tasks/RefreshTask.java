package eu.toolchain.exposr.tasks;

import eu.toolchain.exposr.project.manager.ProjectManagerRefreshed;
import eu.toolchain.exposr.project.manager.RefreshableProjectManager;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;

public class RefreshTask implements Task<ProjectManagerRefreshed> {
    private final RefreshableProjectManager projectManager;

    public RefreshTask(RefreshableProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    @Override
    public ProjectManagerRefreshed run(TaskState state) throws Exception {
        state.system("Refreshing project manager: " + projectManager);
        return projectManager.refreshNow();
    }
}
