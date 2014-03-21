package eu.toolchain.exposr.repository;

import java.util.List;

import eu.toolchain.exposr.project.Project;

public interface Repository {
    public long sync(final Project project);

    public List<Long> syncAll();

    public long build(Project project);

    public List<Long> buildAll();
}
