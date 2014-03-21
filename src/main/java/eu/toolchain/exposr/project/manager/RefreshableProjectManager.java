package eu.toolchain.exposr.project.manager;

import eu.toolchain.exposr.taskmanager.HandleBuilder;

public interface RefreshableProjectManager extends ProjectManager {
    public HandleBuilder<Void> refresh();
}
