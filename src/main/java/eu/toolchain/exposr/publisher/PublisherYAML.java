package eu.toolchain.exposr.publisher;

import eu.toolchain.exposr.yaml.ValidationException;

public interface PublisherYAML {
    public Publisher build(String context) throws ValidationException;
}