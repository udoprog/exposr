package eu.toolchain.exposr.publisher;

import static eu.toolchain.exposr.yaml.Utils.toDirectory;
import static eu.toolchain.exposr.yaml.Utils.toURI;

import java.net.URI;
import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.yaml.ValidationException;

public class RemotePublisherYAML implements PublisherYAML {
    public static final String TYPE = "!remote-publisher";

    @Getter
    @Setter
    private String path;

    @Getter
    @Setter
    private String url;

    @Override
    public Publisher build(String context) throws ValidationException {
        final Path path = toDirectory(context + ".path", this.path);
        final URI u = toURI(context + ".url", this.url);
        return new RemotePublisher(path, u);
    }
}