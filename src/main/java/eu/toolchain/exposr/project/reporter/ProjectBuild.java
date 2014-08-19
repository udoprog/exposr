package eu.toolchain.exposr.project.reporter;

import java.util.Date;

import lombok.Data;

@Data
public class ProjectBuild {
    private final long taskId;
    private final Date date;
    private final Throwable error;
}