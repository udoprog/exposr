package eu.toolchain.exposr.yaml;

import static eu.toolchain.exposr.yaml.Utils.makeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.builder.LocalBuilderYAML;
import eu.toolchain.exposr.project.BasicProjectAuthYAML;
import eu.toolchain.exposr.project.manager.GithubProjectManagerYAML;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.manager.StaticProjectManagerYAML;
import eu.toolchain.exposr.project.reporter.MemoryProjectReporterYAML;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.publisher.LocalPublisherYAML;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.publisher.RemotePublisherYAML;
import eu.toolchain.exposr.repository.LocalRepositoryYAML;
import eu.toolchain.exposr.repository.Repository;

@ToString(of = { "projectManager", "repository", "publisher", "builder",
        "projectReporter" })
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

    private static final TypeDescription[] types = new TypeDescription[] {
            makeType(GithubProjectManagerYAML.class),
            makeType(StaticProjectManagerYAML.class),
            makeType(LocalRepositoryYAML.class),
            makeType(BasicProjectAuthYAML.class),
            makeType(LocalPublisherYAML.class),
            makeType(RemotePublisherYAML.class),
            makeType(LocalBuilderYAML.class),
            makeType(MemoryProjectReporterYAML.class), };

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

        final ExposrConfigYAML configYaml = yaml.loadAs(
                Files.newInputStream(path), ExposrConfigYAML.class);

        return configYaml.build();
    }
}
