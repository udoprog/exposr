package eu.toolchain.exposr.taskmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Allows for the creation of handles reacting to group of tasks.
 * 
 * @author udoprog
 * 
 * @param <T>
 *            The value type that is expected as a result from any of the tasks.
 */
@Slf4j
public class SetupTaskGroup<T> {
    public static class TaskResult<T> {
        @Getter
        private final TaskSnapshot task;

        @Getter
        private final T value;

        public TaskResult(TaskSnapshot task, T value) {
            this.task = task;
            this.value = value;
        }
    }

    public static class TaskError {
        @Getter
        private final TaskSnapshot task;

        @Getter
        private final Throwable error;

        public TaskError(TaskSnapshot task, Throwable error) {
            this.task = task;
            this.error = error;
        }
    }

    public static interface Handle<T> {
        void done(Collection<TaskResult<T>> results,
                Collection<TaskError> errors);
    }

    private final List<TaskSetup<T>> builders;

    private final AtomicInteger count;
    private final List<Handle<T>> handles = new LinkedList<Handle<T>>();

    private final ConcurrentLinkedQueue<TaskResult<T>> results = new ConcurrentLinkedQueue<TaskResult<T>>();
    private final ConcurrentLinkedQueue<TaskError> errors = new ConcurrentLinkedQueue<TaskError>();

    public SetupTaskGroup(List<TaskSetup<T>> builders) {
        this.builders = builders;
        this.count = new AtomicInteger(builders.size());

        for (TaskSetup<T> builder : builders) {
            builder.callback(new TaskSetup.Handle<T>() {
                @Override
                public void done(TaskSnapshot task, T value) {
                    results.add(new TaskResult<T>(task, value));

                    if (count.decrementAndGet() != 0) {
                        return;
                    }

                    finish();
                }

                @Override
                public void error(TaskSnapshot task, Throwable t) {
                    errors.add(new TaskError(task, t));

                    if (count.decrementAndGet() != 0) {
                        return;
                    }

                    finish();
                }
            });
        }
    }

    public SetupTaskGroup<T> callback(Handle<T> handle) {
        this.handles.add(handle);
        return this;
    }

    public SetupTaskGroup<T> parentId(Long parentId) {
        for (TaskSetup<T> builder : builders) {
            builder.parentId(parentId);
        }

        return this;
    }

    public List<Long> execute() {
        final List<Long> ids = new ArrayList<Long>();

        if (builders.isEmpty()) {
            finish();
            return ids;
        }

        for (TaskSetup<T> builder : builders) {
            ids.add(builder.execute());
        }

        return ids;
    }

    private void finish() {
        final Collection<TaskError> errors = new ArrayList<TaskError>(
                this.errors);
        final Collection<TaskResult<T>> results = new ArrayList<TaskResult<T>>(
                this.results);

        for (Handle<T> handle : handles) {
            try {
                handle.done(results, errors);
            } catch (Throwable t) {
                log.error("Failed to call handle");
            }
        }
    }
}
