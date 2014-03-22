package eu.toolchain.exposr.project.reporter;

import java.util.Date;

import lombok.Getter;

public class ProjectSync {
    @Getter
    private final long taskId;

    @Getter
    private final Date date;

    @Getter
    private final String id;

    @Getter
    private final Throwable error;

    public ProjectSync(long taskId, Date date, String id, Throwable error) {
        this.taskId = taskId;
        this.date = date;
        this.id = id;
        this.error = error;
    }
}