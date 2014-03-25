package eu.toolchain.exposr.project.manager;

import lombok.Getter;

public class ProjectManagerRefreshed {
    @Getter
    private final boolean changed;

    public ProjectManagerRefreshed(boolean changed) {
        this.changed = changed;
    }
}
