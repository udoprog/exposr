package eu.toolchain.exposr.publisher;

import static eu.toolchain.exposr.yaml.Utils.notEmpty;
import lombok.Getter;
import lombok.Setter;

public class LocalPublisherYAML implements PublisherYAML {
    public static final String TYPE = "!local-publisher";

    @Getter
    @Setter
    private String path;

    @Override
    public Publisher build(String context) {
        notEmpty(context + ".path", path);
        return new LocalPublisher(path);
    }
}