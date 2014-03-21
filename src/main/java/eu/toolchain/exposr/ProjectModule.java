package eu.toolchain.exposr;

import com.google.inject.AbstractModule;
import eu.toolchain.exposr.project.ProjectManager;

public class ProjectModule extends AbstractModule {
    private final ProjectManager projectManager;

    public ProjectModule(final ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    @Override
    protected void configure() {
        bind(ProjectManager.class).toInstance(projectManager);
    }
}