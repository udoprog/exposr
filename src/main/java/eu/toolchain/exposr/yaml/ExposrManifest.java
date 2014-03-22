package eu.toolchain.exposr.yaml;

import java.util.List;

import lombok.Getter;

public class ExposrManifest {
    @Getter
    private final List<String> commands;

    @Getter
    private final List<String> publish;

    public ExposrManifest(List<String> commands, List<String> publish) {
        this.commands = commands;
        this.publish = publish;
    }
}
