package eu.toolchain.exposr.publisher;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.UtilsYAML;
import eu.toolchain.exposr.yaml.ValidationException;

@Slf4j
public class RemotePublisher implements Publisher {
    @Data
    @NoArgsConstructor
    public static class YAML implements Publisher.YAML {
        public static final String TYPE = "!remote-publisher";
        private String path;
        private String url;

        @Override
        public Publisher build(String context) throws ValidationException {
            final Path path = UtilsYAML.toDirectory(context + ".path",
                    this.path);
            final URI u = UtilsYAML.toURI(context + ".url", this.url);
            return new RemotePublisher(path, u);
        }
    }

    private final Path path;
    private final URI uri;

    public RemotePublisher(Path path, URI uri) {
        this.path = path;
        this.uri = uri;
    }

    @Override
    public void publish(final String name, final String id,
            final List<Path> paths, final TaskState state)
            throws ProjectPublishException {
        final Path p = LocalPublisher.buildPublishDirectory(path,
                name, id, paths, state);
        final Path zip = p.getParent().resolve(p.getFileName() + ".zip");

        if (Files.isRegularFile(zip)) {
            state.system("Deleting old zip file: " + zip);

            try {
                Files.delete(zip);
            } catch (IOException e) {
                throw new ProjectPublishException(
                        "Failed to delete old zip file: " + zip);
            }
        }

        try {
            state.system("Creating zip file: " + zip);
            LocalPublisher.buildZipFile(p, zip);
        } catch (ZipException e) {
            throw new ProjectPublishException("Failed to create zip file: "
                    + zip);
        }
    }

    private void uploadPath(final String name, final String id, final Path path) {
        final Client client = ClientBuilder.newClient();

        client.register(MultiPartFeature.class);

        final WebTarget target = client.target(uri).path(
                uri.getPath() + "_exposr/publish/" + name + "/" + id);

        final FileDataBodyPart filePart = new FileDataBodyPart("file",
                path.toFile(),
                MediaType.APPLICATION_OCTET_STREAM_TYPE);

        final MultiPart multipart = new FormDataMultiPart().bodyPart(filePart);

        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.entity(multipart, multipart.getMediaType()));
    }
}
