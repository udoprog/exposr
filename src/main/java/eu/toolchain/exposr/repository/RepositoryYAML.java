package eu.toolchain.exposr.repository;

import eu.toolchain.exposr.yaml.ValidationException;

public interface RepositoryYAML {
    public Repository build(String context) throws ValidationException;
}