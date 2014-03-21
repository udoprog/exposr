package eu.toolchain.exposr.yaml;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class ExposrYAML {
    @Getter
    @Setter
    private List<String> commands;

    @Getter
    @Setter
    private List<String> publish;
}
