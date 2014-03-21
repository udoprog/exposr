package eu.toolchain.exposr.builder;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectBuildException;
import eu.toolchain.exposr.taskmanager.StreamReader;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.yaml.ExposrYAML;

@Slf4j
public class LocalBuilder implements Builder {
    @Override
    public void execute(final Project project, final ExposrYAML manifest,
            final Path buildPath, final TaskState state)
            throws ProjectBuildException {
        for (final String command : manifest.getCommands()) {
            log.info(project + ": execute: " + command);

            final String[] parts = command.split(" ");

            final ProcessBuilder builder = new ProcessBuilder()
                    .redirectOutput(Redirect.PIPE).redirectError(Redirect.PIPE)
                    .command(parts).directory(buildPath.toFile());

            final Process p;

            try {
                p = builder.start();
            } catch (IOException e) {
                throw new ProjectBuildException("Failed to run command: "
                        + command, e);
            }

            final StreamReader stdout = new StreamReader(p.getInputStream(),
                    new StreamReader.Handle() {
                        @Override
                        public void line(String line) {
                            state.error(line);
                        }
                    });

            final StreamReader stderr = new StreamReader(p.getErrorStream(),
                    new StreamReader.Handle() {
                        @Override
                        public void line(String line) {
                            state.error(line);
                        }
                    });

            stdout.start();
            stderr.start();

            final int status;

            try {
                status = p.waitFor();
            } catch (InterruptedException e) {
                throw new ProjectBuildException("Command interrupted: "
                        + command, e);
            }

            try {
                stdout.join();
            } catch (InterruptedException e) {
                log.error("stdout consumer join failed", e);
            }

            try {
                stderr.join();
            } catch (InterruptedException e) {
                log.error("stderr consumer join failed", e);
            }

            if (status != 0) {
                throw new ProjectBuildException(
                        "Command exited with non-zero exit status [" + status
                                + "]: " + command);
            }
        }
    }
}
