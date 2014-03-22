package eu.toolchain.exposr.project.reporter;

import java.util.Date;
import java.util.List;

import eu.toolchain.exposr.project.Project;

public class MemoryProjectReporter implements ProjectReporter {
    private final MemoryProjectLog<ProjectSync> syncs = new MemoryProjectLog<ProjectSync>();
    private final MemoryProjectLog<ProjectBuild> builds = new MemoryProjectLog<ProjectBuild>();

    @Override
    public void reportSync(long taskId, Project project, String id,
            Throwable error) {
        syncs.append(project, new ProjectSync(taskId, new Date(), id, error));
    }

    @Override
    public ProjectSync getLastSync(Project project) {
        return syncs.getLast(project);
    }

    @Override
    public List<ProjectSync> getSyncs(Project project) {
        return syncs.all(project);
    }

    @Override
    public void reportBuild(long taskId, Project project, Throwable error) {
        builds.append(project, new ProjectBuild(taskId, new Date(), error));
    }

    @Override
    public ProjectBuild getLastBuild(Project project) {
        return builds.getLast(project);
    }

    @Override
    public List<ProjectBuild> getBuilds(Project project) {
        return builds.all(project);
    }
}
