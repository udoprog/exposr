package eu.toolchain.exposr.publisher;

import static eu.toolchain.exposr.yaml.Utils.toDirectory;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.yaml.ValidationException;

public class LocalPublisherYAML implements PublisherYAML {
    public static final String TYPE = "!local-publisher";

    @Getter
    @Setter
    private String path;

    @Override
    public Publisher build(String context) throws ValidationException {
        final Path p = toDirectory(context + ".path", this.path);
        return new LocalPublisher(p);
    }
}