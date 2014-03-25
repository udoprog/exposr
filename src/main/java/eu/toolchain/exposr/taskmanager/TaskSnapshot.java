package eu.toolchain.exposr.taskmanager;

import java.util.Date;
import java.util.List;

import lombok.Getter;

/**
 * An immutable snapshot of TaskState.
 * 
 * @author udoprog
 */
public final class TaskSnapshot {
    @Getter
    final long id;

    @Getter
    final String title;

    @Getter
    final private Date started;

    @Getter
    final private Date ended;

    @Getter
    final Long parentId;

    @Getter
    final private Throwable error;

    @Getter
    final private List<TaskOutput> output;

    @Getter
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