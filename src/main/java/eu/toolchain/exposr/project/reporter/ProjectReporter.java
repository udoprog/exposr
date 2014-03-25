package eu.toolchain.exposr.project.reporter;

import java.util.List;

import eu.toolchain.exposr.project.Project;

public interface ProjectReporter {
    public static interface YAML {
        public ProjectReporter build(String context);
    }

    public void reportSync(long taskId, Project project, String id,
            Throwable error);
    public List<ProjectSync> getSyncs(Project project);
    public ProjectSync getLastSync(Project project);

    public void reportBuild(long taskId, Project project, Throwable error);
    public List<ProjectBuild> getBuilds(Project project);
    public ProjectBuild getLastBuild(Project project);
}
