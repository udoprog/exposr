package eu.toolchain.exposr.tasks;

import lombok.Data;
import eu.toolchain.exposr.project.manager.ProjectManagerRefreshed;
import eu.toolchain.exposr.project.manager.RefreshableProjectManager;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;

@Data
public class RefreshTask implements Task<ProjectManagerRefreshed> {
    private final RefreshableProjectManager manager;

    @Override
    public ProjectManagerRefreshed run(TaskState state) throws Exception {
        state.system("Refreshing project manager: " + manager);
        return manager.refreshNow();
    }
}
