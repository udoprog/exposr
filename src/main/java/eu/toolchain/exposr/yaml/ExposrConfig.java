package eu.toolchain.exposr.yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Data;
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

@Data
public class ExposrConfig {
    @Slf4j
    @Data
    public static class YAML {
        private ProjectManager.YAML projectManager;
        private Repository.YAML repository;
        private Publisher.YAML publisher;
        private Builder.YAML builder;
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
            return new ExposrConfig(setupProjectManager(),
                    setupRepository(),
                    setupPublisher(),
                    setupBuilder(),
                    setupProjectReporter());
        }
    }

    private final ProjectManager projectManager;
    private final Repository repository;
    private final Publisher publisher;
    private final Builder builder;
    private final ProjectReporter projectReporter;

    private static final TypeDescription[] types = new TypeDescription[] {
            ConfigUtils.makeType(GithubProjectManager.YAML.class),
            ConfigUtils.makeType(StaticProjectManager.YAML.class),
            ConfigUtils.makeType(LocalRepository.YAML.class),
            ConfigUtils.makeType(BasicProjectAuth.YAML.class),
            ConfigUtils.makeType(LocalPublisher.YAML.class),
            ConfigUtils.makeType(RemotePublisher.YAML.class),
            ConfigUtils.makeType(LocalBuilder.YAML.class),
            ConfigUtils.makeType(MemoryProjectReporter.YAML.class), };

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
