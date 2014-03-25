package eu.toolchain.exposr.taskmanager;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Track what happens with a single task.
 * 
 * @author udoprog
 * 
 * @param <T>
 *            Type of the value returned by the task.
 */
@Slf4j
public final class TaskTracker<T> implements Runnable {
    private final TaskManager manager;
    private final TaskState state;
    private final Task<T> task;
    private final List<Task.Done<T>> onDone;
    private final List<Task.Error> onError;

    public TaskTracker(TaskManager manager, TaskState state, Task<T> task,
            List<Task.Done<T>> onDone, List<Task.Error> onError) {
        this.manager = manager;
        this.state = state;
        this.task = task;
        this.onDone = onDone;
        this.onError = onError;
    }

    @Override
    public void run() {
        final T result;

        try {
            result = runTask();
        } catch (Throwable t) {
            handleError(t);
            return;
        }

        handleResult(result);
    }

    private T runTask() throws Exception {
        if (log.isDebugEnabled())
            log.debug(String.format("Task ID[%d] RUN %s", state.getId(), task));

        state.start();
        return task.run(state);
    }

    private void handleResult(final T result) {
        if (log.isDebugEnabled())
            log.debug(String.format("Task ID[%d] END %s", state.getId(), task));

        final TaskSnapshot snapshot = state.end();
        this.manager.end(this.state.getId(), snapshot);

        for (final Task.Done<T> callback : onDone) {
            try {
                callback.done(snapshot, result);
            } catch (Throwable t) {
                log.error("Problem when invoking a done callback", t);
            }
        }
    }

    private void handleError(Throwable error) {
        if (log.isDebugEnabled())
            log.debug(String.format("Task ID[%d] ERROR %s", state.getId(),
                    task));

        final TaskSnapshot snapshot = state.end(error);
        this.manager.end(state.getId(), snapshot);

        for (final Task.Error e : onError) {
            try {
                e.error(snapshot, error);
            } catch (Throwable t) {
                log.error("Problem when invoking an error callback", t);
            }
        }
    }
}