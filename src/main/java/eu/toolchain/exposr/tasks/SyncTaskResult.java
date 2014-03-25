package eu.toolchain.exposr.tasks;

import lombok.Getter;
import lombok.ToString;

import org.eclipse.jgit.lib.ObjectId;

@ToString(of = { "updated", "id" })
public class SyncTaskResult {
    @Getter
    private final boolean updated;
    @Getter
    private final ObjectId id;

    public SyncTaskResult(boolean updated, ObjectId id) {
        this.updated = updated;
        this.id = id;
    }
}