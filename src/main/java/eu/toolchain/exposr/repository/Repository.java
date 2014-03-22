package eu.toolchain.exposr.repository;

import java.io.InputStream;
import java.util.List;

import eu.toolchain.exposr.project.Project;

public interface Repository {
    public long sync(Project project);

    public List<Long> syncAll();

    public long build(Project project);

    public List<Long> buildAll();

    public long deploy(String name, String id, InputStream inputStream);
}
