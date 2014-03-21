package eu.toolchain.exposr.project;

import java.util.Date;

import lombok.Getter;

public class ProjectBuild {
    @Getter
    private final Date date;

    @Getter
    private final boolean success;

    @Getter
    private final Throwable error;

    public ProjectBuild(Date date, Throwable error) {
        this.date = date;
        this.success = error == null;
        this.error = error;
    }
}