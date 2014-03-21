package eu.toolchain.exposr.yaml;

import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.repository.Repository;

public class ExposrConfig {
    @Getter
    @Setter
    private ProjectManager projectManager;

    @Getter
    @Setter
    private Repository repository;

    @Getter
    @Setter
    private Publisher publisher;

    @Getter
    @Setter
    private Builder builder;

    @Getter
    @Setter
    private ProjectReporter projectReporter;
}
