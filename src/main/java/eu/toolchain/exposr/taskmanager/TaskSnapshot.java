package eu.toolchain.exposr.taskmanager;

import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * An immutable snapshot of TaskState.
 *
 * @author udoprog
 */
@Data
public final class TaskSnapshot {
    final long id;
    final String title;
    final private Date started;
    final private Date ended;
    final Long parentId;
    final private Throwable error;
    final private List<TaskOutput> output;
    final private long duration;

    public TaskSnapshot(long id, String title, Date started, Long parentId,
            Date ended, Throwable error, List<TaskOutput> output) {
        this.id = id;
        this.title = title;
        this.started = started;
        this.parentId = parentId;
        this.ended = ended;
        this.error = error;
        this.output = output;
        this.duration = ended.getTime() - started.getTime();
    }
}