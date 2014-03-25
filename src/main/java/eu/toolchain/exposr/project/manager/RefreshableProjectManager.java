package eu.toolchain.exposr.project.manager;

import eu.toolchain.exposr.project.ProjectException;
import eu.toolchain.exposr.taskmanager.TaskSetup;

public interface RefreshableProjectManager extends ProjectManager {
    public TaskSetup<ProjectManagerRefreshed> refresh();

    public ProjectManagerRefreshed refreshNow() throws ProjectException;
}
