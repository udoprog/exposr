package eu.toolchain.exposr.project;

import java.util.Date;
import java.util.List;

public class InMemoryProjectReporter implements ProjectReporter {
    private final ProjectLog<ProjectSync> syncs = new ProjectLog<ProjectSync>();
    private final ProjectLog<ProjectBuild> builds = new ProjectLog<ProjectBuild>();

    @Override
    public void reportSync(Project project, String id, Throwable error) {
        syncs.append(project, new ProjectSync(new Date(), id, error));
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
    public void reportBuild(Project project, Throwable error) {
        builds.append(project, new ProjectBuild(new Date(), error));
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
