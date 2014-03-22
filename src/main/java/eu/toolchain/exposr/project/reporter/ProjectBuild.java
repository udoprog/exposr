package eu.toolchain.exposr.project.reporter;

import java.util.Date;

import lombok.Getter;

public class ProjectBuild {
    @Getter
    private final long taskId;

    @Getter
    private final Date date;

    @Getter
    private final Throwable error;

    public ProjectBuild(long taskId, Date date, Throwable error) {
        this.taskId = taskId;
        this.date = date;
        this.error = error;
    }
}