package eu.toolchain.exposr.project.manager;

import eu.toolchain.exposr.project.ProjectException;
import eu.toolchain.exposr.taskmanager.SetupTask;

public interface RefreshableProjectManager extends ProjectManager {
    public SetupTask<ProjectManagerRefreshed> refresh();

    public ProjectManagerRefreshed refreshNow() throws ProjectException;
}
