package eu.toolchain.exposr.project;

import java.util.Date;

import lombok.Getter;

public class ProjectSync {
    @Getter
    private final Date date;
    @Getter
    private final String id;
    @Getter
    private final boolean success;
    @Getter
    private final Throwable error;

    public ProjectSync(Date date, String id, Throwable error) {
        this.date = date;
        this.id = id;
        this.success = error == null;
        this.error = error;
    }
}