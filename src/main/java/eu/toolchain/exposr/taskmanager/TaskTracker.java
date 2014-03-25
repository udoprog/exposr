package eu.toolchain.exposr.taskmanager;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import eu.toolchain.exposr.taskmanager.TaskSetup.OnDone;
import eu.toolchain.exposr.taskmanager.TaskSetup.OnError;

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
    private final List<OnDone<T>> done;
    private final List<OnError> error;

    public TaskTracker(TaskManager manager, TaskState state, Task<T> task,
            List<OnDone<T>> done, List<OnError> error) {
        this.manager = manager;
        this.state = state;
        this.task = task;
        this.done = done;
        this.error = error;
    }

    @Override
    public void run() {
        this.state.start();

        final T result;

        try {
            result = this.task.run(state);
        } catch (Throwable t) {
            final TaskSnapshot snapshot = this.state.end(t);
            this.manager.end(this.state.getId(), snapshot);

            for (OnError callback : error) {
                try {
                    callback.error(snapshot, t);
                } catch (Throwable t2) {
                    log.error("Error when invoking 'error' callback", t2);
                }
            }

            return;
        }

        final TaskSnapshot snapshot = this.state.end(null);
        this.manager.end(this.state.getId(), snapshot);

        for (OnDone<T> callback : done) {
            try {
                callback.done(snapshot, result);
            } catch (Throwable t) {
                log.error("Error when invoking 'done' callback", t);
            }
        }
    }
}