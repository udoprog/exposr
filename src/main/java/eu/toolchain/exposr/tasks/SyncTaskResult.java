package eu.toolchain.exposr.tasks;

import lombok.Data;

import org.eclipse.jgit.lib.ObjectId;

@Data
public class SyncTaskResult {
    private final boolean updated;
    private final ObjectId id;
}