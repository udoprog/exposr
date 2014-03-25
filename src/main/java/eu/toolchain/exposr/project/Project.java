package eu.toolchain.exposr.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import lombok.EqualsAndHashCode;
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
import org.eclipse.jgit.transport.RefSpec;

@Slf4j
@ToString(of = { "name", "url", "ref" })
@EqualsAndHashCode(of = { "name", "url", "ref" })
public class Project {
    private final String name;
    private final String url;
    private final String ref;
    private final ProjectAuth auth;

    public Project(final String name, final String url, final String ref,
            final ProjectAuth auth) {
        this.name = name;
        this.url = url;
        this.ref = ref;
        this.auth = auth;
    }

    public String getName() {
        return name;
    }

    public ObjectId findRemoteId() throws ProjectException {
        return realFindRemoteRef();
    }

    private ObjectId realFindRemoteRef() throws ProjectException {
        final Collection<Ref> remoteRefs;

        final LsRemoteCommand lsRemote = Git.lsRemoteRepository()
                .setRemote(url);

        if (auth != null)
            lsRemote.setCredentialsProvider(auth.build());

        try {
            remoteRefs = lsRemote.call();
        } catch (GitAPIException e) {
            throw new ProjectException("ls-remote failed for: " + url, e);
        }

        for (final Ref remoteRef : remoteRefs) {
            if (!ref.equals(remoteRef.getName())) {
                continue;
            }

            return remoteRef.getObjectId();
        }

        return null;
    }

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
            localId = git.getRepository().resolve(ref);
        } catch (IOException e) {
            throw new ProjectException("Failed to resolve local reference", e);
        }

        if (localId == null || !localId.equals(remoteId)) {
            log.info(this + ": Fetching remote object: " + remoteId);

            final FetchCommand fetchCommand = git.fetch().setRemote(url)
                    .setRefSpecs(new RefSpec(ref));

            if (auth != null)
                fetchCommand.setCredentialsProvider(auth.build());

            try {
                fetchCommand.call();
            } catch (GitAPIException e) {
                throw new ProjectException(this
                        + ": Failed to fetch remote ref", e);
            }
        }
    }

    public ObjectId clone(Path directory) throws ProjectException {
        return realClone(directory);
    }

    private ObjectId realClone(Path directory) throws ProjectException {
        log.info(this + ": Cloning to: " + directory);

        final CloneCommand cloneCommand = Git.cloneRepository()
                .setDirectory(directory.toFile()).setURI(url).setBranch(ref);

        if (auth != null)
            cloneCommand.setCredentialsProvider(auth.build());

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