package eu.toolchain.exposr.project;

import java.util.Date;

import lombok.Getter;

public class ProjectError {
    @Getter
    private final Date date;
    @Getter
    private final Throwable error;

    public ProjectError(Date date, Throwable error) {
        this.date = date;
        this.error = error;
    }
}