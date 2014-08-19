package eu.toolchain.exposr.project.reporter;

import java.util.Date;

import lombok.Data;

@Data
public class ProjectSync {
    private final long taskId;
    private final Date date;
    private final String id;
    private final Throwable error;
}