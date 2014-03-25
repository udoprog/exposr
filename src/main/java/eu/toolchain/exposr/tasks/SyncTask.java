package eu.toolchain.exposr.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectException;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;

@Slf4j
@ToString(of = { "project", "path" })
public class SyncTask implements Task<SyncTaskResult> {
    private final Project project;
    private final Path path;

    public SyncTask(Project project, Path path) {
        this.project = project;
        this.path = path.toAbsolutePath().normalize();
    }

    private ObjectId fetch(Project project, Path buildPath, ObjectId remoteId)
            throws ProjectException {
        log.info("Syncing {} to {}", project, buildPath);
        return project.fetch(buildPath, remoteId);
    }

    private ObjectId clone(Project project, Path buildPath)
            throws ProjectException {
        log.info("Cloning {} to {}", project, buildPath);
        return project.clone(buildPath);
    }

    @Override
    public SyncTaskResult run(TaskState state) throws Exception {
        final ObjectId remoteId = project.findRemoteId();
        state.system("Synchronizing " + path);

        if (remoteId == null) {
            throw new ProjectException("Remote id not found for project: "
                    + project);
        }

        if (!Files.isDirectory(path)) {
            state.system("Making fresh clone");
            final ObjectId headId = clone(project, path);
            return new SyncTaskResult(true, headId);
        }

        final Git git;

        try {
            git = Git.open(path.toFile());
        } catch (IOException e) {
            log.error("Failed to open local git repo", e);

            if (Files.isDirectory(path)) {
                state.system("Purging build path");

                try {
                    FileUtils.deleteDirectory(path.toFile());
                } catch (IOException io) {
                    throw new ProjectException(
                            "Failed to clean up dirty repo: " + path, io);
                }
            }

            state.system("Making fresh clone");
            final ObjectId headId = clone(project, path);
            return new SyncTaskResult(true, headId);
        }

        final ObjectId newHeadId;

        try {
            newHeadId = git.getRepository().resolve(Constants.HEAD);
        } catch (IOException e) {
            state.system("Fetching remote " + remoteId);
            final ObjectId headId = fetch(project, path, remoteId);
            return new SyncTaskResult(true, headId);
        }

        if (newHeadId == null || !newHeadId.equals(remoteId)) {
            state.system("Fetching remote " + remoteId);
            final ObjectId headId = fetch(project, path, remoteId);
            return new SyncTaskResult(true, headId);
        }

        state.system("Nothing has changed");
        return new SyncTaskResult(false, newHeadId);
    }
}
