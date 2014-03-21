package eu.toolchain.exposr.project.github;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectException;

@Slf4j
@ToString(of = { "name", "cloneUrl", "remoteRef" })
public class GitProject implements Project {
    public static interface AuthProvider {
        CredentialsProvider buildCredentials();
    }

    private final String name;
    private final String cloneUrl;
    private final String remoteRef;
    private final AuthProvider authProvider;

    public GitProject(final String name,
            final String cloneUrl,
            final String remoteRef, final AuthProvider authProvider) {
        this.name = name;
        this.cloneUrl = cloneUrl;
        this.remoteRef = remoteRef;
        this.authProvider = authProvider;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ObjectId findRemoteId() throws ProjectException {
        return realFindRemoteRef();
    }

    private ObjectId realFindRemoteRef() throws ProjectException {
        final Collection<Ref> refs;

        final LsRemoteCommand lsRemote = Git.lsRemoteRepository().setRemote(
                cloneUrl);

        if (authProvider != null)
            lsRemote.setCredentialsProvider(authProvider.buildCredentials());

        try {
            refs = lsRemote.call();
        } catch (GitAPIException e) {
            throw new ProjectException("ls-remote failed for: " + cloneUrl, e);
        }

        for (final Ref ref : refs) {
            if (!remoteRef.equals(ref.getName())) {
                continue;
            }

            return ref.getObjectId();
        }

        return null;
    }

    @Override
    public ObjectId fetch(Path directory, ObjectId remoteId)
            throws ProjectException {
        return realFetch(directory, remoteId);
    }

    private ObjectId realFetch(Path directory, ObjectId remoteId)
            throws ProjectException {
        final Git git;

        try {
            git = Git.open(directory.toFile());
        } catch (IOException e) {
            throw new ProjectException(
                    this + ": Failed to open git repository", e);
        }

        fetchIfNeeded(git, remoteId);
        clean(git);
        resetHard(git, remoteId);
        return remoteId;
    }

    private void resetHard(final Git git, ObjectId remoteId)
            throws ProjectException {
        log.info(this + ": Reseting HEAD to: " + remoteId);

        try {
            git.reset().setMode(ResetType.HARD).setRef(remoteId.name()).call();
        } catch (GitAPIException e) {
            throw new ProjectException(this + ": Failed to reset HEAD");
        }
    }

    private void clean(final Git git) throws ProjectException {
        log.info(this + ": Cleaning local repository");

        try {
            git.clean().setIgnore(true).setCleanDirectories(true).call();
        } catch (GitAPIException e) {
            throw new ProjectException(this
                    + ": Failed to clean local repository", e);
        }
    }

    private void fetchIfNeeded(final Git git, ObjectId remoteId)
            throws ProjectException {
        final ObjectId localId;

        try {
            localId = git.getRepository().resolve(remoteRef);
        } catch (IOException e) {
            throw new ProjectException("Failed to resolve local reference", e);
        }

        if (localId == null || !localId.equals(remoteId)) {
            log.info(this + ": Fetching remote object: " + remoteId);

            final FetchCommand fetchCommand = git.fetch().setRemote(cloneUrl)
                    .setRefSpecs(new RefSpec(remoteRef));

            if (authProvider != null)
                fetchCommand.setCredentialsProvider(authProvider
                        .buildCredentials());

            try {
                fetchCommand.call();
            } catch (GitAPIException e) {
                throw new ProjectException(this
                        + ": Failed to fetch remote ref", e);
            }
        }
    }

    @Override
    public ObjectId clone(Path directory) throws ProjectException {
        return realClone(directory);
    }

    private ObjectId realClone(Path directory) throws ProjectException {
        log.info(this + ": Cloning to: " + directory);

        final CloneCommand cloneCommand = Git.cloneRepository()
                .setDirectory(directory.toFile()).setURI(cloneUrl)
                .setBranch(remoteRef);

        if (authProvider != null)
            cloneCommand
                    .setCredentialsProvider(authProvider.buildCredentials());

        final Git git;

        try {
            git = cloneCommand.call();
        } catch (GitAPIException e) {
            throw new ProjectException(this + ": Failed to clone", e);
        }

        final ObjectId headId;

        try {
            headId = git.getRepository().resolve(Constants.HEAD);
        } catch (IOException e) {
            throw new ProjectException(this + ": Failed to resolve HEAD", e);
        }

        if (headId == null) {
            throw new ProjectException(this + ": No ref named "
                    + Constants.HEAD + " found");
        }

        return headId;
    }

    @Override
    public ObjectId getHead(Path directory) throws ProjectException {
        return realGetHead(directory);
    }

    private ObjectId realGetHead(Path directory) throws ProjectException {
        final Git git;

        try {
            git = Git.open(directory.toFile());
        } catch (IOException e) {
            throw new ProjectException(this + ": Failed to open", e);
        }

        final ObjectId headId;

        try {
            headId = git.getRepository().resolve(Constants.HEAD);
        } catch (IOException e) {
            throw new ProjectException(this + ": Failed to resolve HEAD", e);
        }

        if (headId == null) {
            throw new ProjectException(this + ": No reference named "
                    + Constants.HEAD + " found");
        }

        return headId;
    }
}
