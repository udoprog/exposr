package eu.toolchain.exposr.repository;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.yaml.Utils;
import eu.toolchain.exposr.yaml.ValidationException;

public class LocalRepositoryYAML implements RepositoryYAML {
    public static final String TYPE = "!local-repository";

    @Getter
    @Setter
    private String path;

    @Override
    public Repository build(String context) throws ValidationException {
        final Path path = Utils.toPath(context + ".path", this.path);
        return new LocalRepository(path);
    }
}
