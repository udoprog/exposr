package eu.toolchain.exposr.project;

import java.nio.file.Path;

import org.eclipse.jgit.lib.ObjectId;

public interface Project {
    public String getName();

    public ObjectId findRemoteId() throws ProjectException;

    public ObjectId fetch(Path directory, ObjectId remoteId)
            throws ProjectException;

    public ObjectId clone(Path directory) throws ProjectException;

    public ObjectId getHead(Path directory) throws ProjectException;
}
