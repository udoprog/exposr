package eu.toolchain.exposr.yaml;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.builder.BuilderYAML;
import eu.toolchain.exposr.builder.LocalBuilder;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.manager.ProjectManagerYAML;
import eu.toolchain.exposr.project.reporter.MemoryProjectReporter;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.project.reporter.ProjectReporterYAML;
import eu.toolchain.exposr.publisher.LocalPublisher;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.publisher.PublisherYAML;
import eu.toolchain.exposr.repository.LocalRepository;
import eu.toolchain.exposr.repository.Repository;
import eu.toolchain.exposr.repository.RepositoryYAML;

@Slf4j
public class ExposrConfigYAML {
    @Getter
    @Setter
    private ProjectManagerYAML projectManager;

    public ProjectManager setupProjectManager() throws ValidationException {
        if (projectManager == null)
            throw new ValidationException("projectManager: must be defined");

        return projectManager.build("projectManager");
    }

    @Getter
    @Setter
    private RepositoryYAML repository;

    public Repository setupRepository() throws ValidationException {
        if (repository == null) {
            final Repository repository = new LocalRepository(
                    LocalRepository.DEFAULT_PATH);
            log.warn("Using default repository: " + repository);
            return repository;
        }

        return repository.build("repository");
    }

    @Getter
    @Setter
    private PublisherYAML publisher;

    public Publisher setupPublisher() throws ValidationException {
        if (publisher == null) {
            final Publisher publisher = new LocalPublisher(
                    LocalPublisher.DEFUALT_PATH);
            log.info("Using default publisher: " + publisher);
            return publisher;
        }

        return publisher.build("publisher");
    }

    @Getter
    @Setter
    private BuilderYAML builder;

    public Builder setupBuilder() {
        if (builder == null)
            return new LocalBuilder();

        return builder.build("builder");
    }

    @Getter
    @Setter
    private ProjectReporterYAML projectReporter;

    public ProjectReporter setupProjectReporter() {
        if (projectReporter == null)
            return new MemoryProjectReporter();

        return projectReporter.build("projectReporter");
    }

    public ExposrConfig build() throws ValidationException {
        final ExposrConfig config = new ExposrConfig();
        config.setProjectManager(setupProjectManager());
        config.setRepository(setupRepository());
        config.setPublisher(setupPublisher());
        config.setBuilder(setupBuilder());
        config.setProjectReporter(setupProjectReporter());
        return config;
    }
}
