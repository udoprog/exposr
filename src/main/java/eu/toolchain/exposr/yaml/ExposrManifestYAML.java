package eu.toolchain.exposr.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.yaml.snakeyaml.Yaml;

import eu.toolchain.exposr.project.ProjectException;

@Data
public class ExposrManifestYAML {
    private static final Yaml yaml = new Yaml();

    private List<String> commands;
    private List<String> publish;

    public ExposrManifest build() throws ValidationException {
        UtilsYAML.notEmpty("commands", commands);
        final List<String> publish = UtilsYAML.toList("publish", this.publish);
        return new ExposrManifest(commands, publish);
    }

    public static ExposrManifest parse(final Path path)
            throws ProjectException {
        final InputStream inputStream;

        try {
            inputStream = Files.newInputStream(path);
        } catch (IOException e) {
            throw new ProjectException("Failed to open manifest: " + path, e);
        }

        final ExposrManifestYAML manifest;

        try {
            manifest = parseInputStream(inputStream);
        } catch (Throwable error) {
            throw new ProjectException("Failed to parse manifest: " + path,
                    error);
        }

        if (manifest == null) {
            return new ExposrManifest(new ArrayList<String>(),
                    new ArrayList<String>());
        }

        try {
            return manifest.build();
        } catch (ValidationException e) {
            throw new ProjectException("Invalid manifest: " + path, e);
        }
    }

    private static ExposrManifestYAML parseInputStream(InputStream inputStream) {
        synchronized (yaml) {
            return yaml.loadAs(inputStream, ExposrManifestYAML.class);
        }
    }
}
