package eu.toolchain.exposr.yaml;

import static eu.toolchain.exposr.yaml.Utils.notEmpty;
import static eu.toolchain.exposr.yaml.Utils.toList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.yaml.snakeyaml.Yaml;

import eu.toolchain.exposr.project.ProjectException;

public class ExposrManifestYAML {
    private static final ThreadLocal<Yaml> yamls = new ThreadLocal<Yaml>();

    @Getter
    @Setter
    private List<String> commands;

    @Getter
    @Setter
    private List<String> publish;

    public ExposrManifest build() throws ValidationException {
        notEmpty("commands", commands);
        final List<String> publish = toList("publish", this.publish);
        return new ExposrManifest(commands, publish);
    }

    private static Yaml getYaml() {
        Yaml yaml = yamls.get();

        if (yaml != null) {
            return yaml;
        }

        synchronized (yamls) {
            yaml = yamls.get();

            if (yaml != null) {
                return yaml;
            }

            yaml = new Yaml();
            yamls.set(yaml);
        }

        return yaml;
    }

    public static ExposrManifest parse(final Path path)
            throws ProjectException {

        final Yaml yaml = getYaml();

        final InputStream inputStream;

        try {
            inputStream = Files.newInputStream(path);
        } catch (IOException e) {
            throw new ProjectException("Failed to open manifest", e);
        }

        ExposrManifestYAML manifest;

        try {
            manifest = yaml.loadAs(inputStream, ExposrManifestYAML.class);
        } catch (Throwable t) {
            throw new ProjectException("Invalid manifest: " + path, t);
        }

        if (manifest == null) {
            manifest = new ExposrManifestYAML();
        }

        try {
            return manifest.build();
        } catch (ValidationException e) {
            throw new ProjectException("Invalid manifest: " + path, e);
        }
    }
}
