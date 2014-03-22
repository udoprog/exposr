package eu.toolchain.exposr.repository;

import static eu.toolchain.exposr.yaml.Utils.notEmpty;
import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.yaml.ValidationException;

public class LocalRepositoryYAML implements RepositoryYAML {
    public static final String TYPE = "!local-repository";

    @Getter
    @Setter
    private String path;

    @Override
    public Repository build(String context) throws ValidationException {
        notEmpty(context + ".path", path);
        return new LocalRepository(path);
    }
}
