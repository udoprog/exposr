package eu.toolchain.exposr.yaml;

import java.util.List;

import lombok.Data;
import lombok.Getter;

@Data
public class ExposrManifest {
    private final List<String> commands;
    private final List<String> publish;

    public ExposrManifest(List<String> commands, List<String> publish) {
        this.commands = commands;
        this.publish = publish;
    }
}
