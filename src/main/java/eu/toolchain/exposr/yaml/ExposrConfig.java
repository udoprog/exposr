package eu.toolchain.exposr.yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.builder.LocalBuilder;
import eu.toolchain.exposr.project.BasicProjectAuth;
import eu.toolchain.exposr.project.manager.GithubProjectManager;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.manager.StaticProjectManager;
import eu.toolchain.exposr.project.reporter.MemoryProjectReporter;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.publisher.LocalPublisher;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.publisher.RemotePublisher;
import eu.toolchain.exposr.repository.LocalRepository;
import eu.toolchain.exposr.repository.Repository;

@ToString(of = { "projectManager", "repository", "publisher", "builder",
        "projectReporter" })
public class ExposrConfig {
    @Slf4j
    public static class YAML {
        @Getter
        @Setter
        private ProjectManager.YAML projectManager;

        @Getter
        @Setter
        private Repository.YAML repository;

        @Getter
        @Setter
        private Publisher.YAML publisher;

        @Getter
        @Setter
        private Builder.YAML builder;

        @Getter
        @Setter
        private ProjectReporter.YAML projectReporter;

        public ProjectManager setupProjectManager() throws ValidationException {
            if (projectManager == null)
                throw new ValidationException("projectManager: must be defined");

            return projectManager.build("projectManager");
        }

        public Repository setupRepository() throws ValidationException {
            if (repository == null) {
                final Repository repository = new LocalRepository(
                        LocalRepository.DEFAULT_PATH);
                log.warn("Using default repository: " + repository);
                return repository;
            }

            return repository.build("repository");
        }

        public Publisher setupPublisher() throws ValidationException {
            if (publisher == null) {
                final Publisher publisher = new LocalPublisher(
                        LocalPublisher.DEFUALT_PATH, null);
                log.info("Using default publisher: " + publisher);
                return publisher;
            }

            return publisher.build("publisher");
        }

        public Builder setupBuilder() {
            if (builder == null)
                return new LocalBuilder();

            return builder.build("builder");
        }

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

    private static final TypeDescription[] types = new TypeDescription[] {
            UtilsYAML.makeType(GithubProjectManager.YAML.class),
            UtilsYAML.makeType(StaticProjectManager.YAML.class),
            UtilsYAML.makeType(LocalRepository.YAML.class),
            UtilsYAML.makeType(BasicProjectAuth.YAML.class),
            UtilsYAML.makeType(LocalPublisher.YAML.class),
            UtilsYAML.makeType(RemotePublisher.YAML.class),
            UtilsYAML.makeType(LocalBuilder.YAML.class),
            UtilsYAML.makeType(MemoryProjectReporter.YAML.class), };

    private static final class CustomConstructor extends Constructor {
        public CustomConstructor() {
            for (TypeDescription t : types) {
                addTypeDescription(t);
            }
        }
    }

    public static ExposrConfig parse(Path path) throws ValidationException,
            IOException {
        final Yaml yaml = new Yaml(new CustomConstructor());

        final YAML configYaml = yaml.loadAs(Files.newInputStream(path),
                YAML.class);

        return configYaml.build();
    }
}
