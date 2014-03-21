package eu.toolchain.exposr.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectException;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;
import eu.toolchain.exposr.tasks.SyncTask.SyncResult;

@Slf4j
public class SyncTask implements Task<SyncResult> {
    private final Project project;
    private final Path buildPath;

    public static class SyncResult {
        @Getter
        private final boolean updated;
        @Getter
        private final ObjectId id;

        public SyncResult(boolean updated, ObjectId id) {
            this.updated = updated;
            this.id = id;
        }
    }

    public SyncTask(Project project, Path buildPath) {
        this.project = project;
        this.buildPath = buildPath;
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
    public SyncResult run(TaskState state) throws Exception {
        final ObjectId remoteId = project.findRemoteId();
        state.system("Synchronizing " + buildPath);

        if (remoteId == null) {
            throw new ProjectException("Remote id not found for project: "
                    + project);
        }

        if (!Files.isDirectory(buildPath)) {
            state.system("Cloning to " + buildPath);
            final ObjectId headId = clone(project, buildPath);
            return new SyncResult(false, headId);
        }

        final Git git;

        try {
            git = Git.open(buildPath.toFile());
        } catch (IOException e) {
            log.error("Failed to open local git repo", e);

            if (Files.isDirectory(buildPath)) {
                state.system("Cleaning " + buildPath);

                try {
                    FileUtils.deleteDirectory(buildPath.toFile());
                } catch (IOException io) {
                    throw new ProjectException(
                            "Failed to clean up dirty repo: " + buildPath, io);
                }
            }

            state.system("Cloning to " + buildPath);
            final ObjectId headId = clone(project, buildPath);
            return new SyncResult(false, headId);
        }

        final ObjectId newHeadId;

        try {
            newHeadId = git.getRepository().resolve(Constants.HEAD);
        } catch (IOException e) {
            state.system("Fetching " + remoteId + " to " + buildPath);
            final ObjectId headId = fetch(project, buildPath, remoteId);
            return new SyncResult(true, headId);
        }

        if (newHeadId == null || !newHeadId.equals(remoteId)) {
            state.system("Fetching " + remoteId + " to " + buildPath);
            final ObjectId headId = fetch(project, buildPath, remoteId);
            return new SyncResult(true, headId);
        }

        state.system("Nothing has changed in " + buildPath);
        return new SyncResult(false, newHeadId);
    }
}
