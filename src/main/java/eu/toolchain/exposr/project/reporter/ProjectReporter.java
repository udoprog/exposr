package eu.toolchain.exposr.project.reporter;

import java.util.List;

import eu.toolchain.exposr.project.Project;

public interface ProjectReporter {
    public void reportSync(Project project, String id, Throwable error);
    public List<ProjectSync> getSyncs(Project project);
    public ProjectSync getLastSync(Project project);

    public void reportBuild(Project project, Throwable error);
    public List<ProjectBuild> getBuilds(Project project);
    public ProjectBuild getLastBuild(Project project);
}