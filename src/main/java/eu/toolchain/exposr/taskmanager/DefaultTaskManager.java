package eu.toolchain.exposr.taskmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

import eu.toolchain.exposr.taskmanager.HandleBuilder.OnDone;
import eu.toolchain.exposr.taskmanager.HandleBuilder.OnError;

@Slf4j
public class DefaultTaskManager implements TaskManager {
    final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private long id = 0;

    private final Map<Long, TaskState> pending = new HashMap<Long, TaskState>();
    private final Map<Long, TaskSnapshot> finished = new HashMap<Long, TaskSnapshot>();

    private static final class TaskTracker<T> implements Runnable {
        private final DefaultTaskManager manager;
        private final TaskState state;
        private final Task<T> task;
        private final List<OnDone<T>> done;
        private final List<OnError> error;

        public TaskTracker(DefaultTaskManager manager, TaskState state,
                Task<T> task,
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
                this.state.end(t);
                this.manager.end(this.state.getId());

                for (OnError callback : error) {
                    try {
                        callback.error(t);
                    } catch (Throwable t2) {
                        log.error("Error when invoking 'error' callback", t2);
                    }
                }

                return;
            }

            this.state.end();
            this.manager.end(this.state.getId());

            for (OnDone<T> callback : done) {
                try {
                    callback.done(result);
                } catch (Throwable t) {
                    log.error("Error when invoking 'done' callback", t);
                }
            }
        }
    }

    public static class DefaultHandleBuilder<T> implements HandleBuilder<T> {
        private final DefaultTaskManager manager;
        private final String title;
        private final Task<T> task;
        private final List<OnDone<T>> done = new LinkedList<OnDone<T>>();
        private final List<OnError> error = new LinkedList<OnError>();

        public DefaultHandleBuilder(DefaultTaskManager manager, String title,
                Task<T> task) {
            this.manager = manager;
            this.title = title;
            this.task = task;
        }

        @Override
        public HandleBuilder<T> callback(Handle<T> callback) {
            done.add(callback);
            error.add(callback);
            return this;
        }

        @Override
        public HandleBuilder<T> done(OnDone<T> callback) {
            done.add(callback);
            return this;
        }

        @Override
        public HandleBuilder<T> error(OnError callback) {
            error.add(callback);
            return this;
        }

        @Override
        public long execute() {
            return manager.execute(title, task, done, error);
        }
    }

    /* (non-Javadoc)
     * @see eu.toolchain.exposr.taskmanager.TaskManager#build(java.lang.String, eu.toolchain.exposr.taskmanager.Task)
     */
    @Override
    public <T> DefaultHandleBuilder<T> build(String title, Task<T> task) {
        return new DefaultHandleBuilder<T>(this, title, task);
    }

    private <T> long execute(String title, Task<T> task, List<OnDone<T>> done,
            List<OnError> error) {
        final long taskId;
        final TaskState state;

        synchronized (pending) {
            taskId = this.id++;
            state = new TaskState(taskId, title);
            pending.put(taskId, state);
        }

        if (log.isDebugEnabled()) {
            log.debug("Executing task [" + title + "] with id [" + taskId + "]");
        }

        final TaskTracker<T> tracker = new TaskTracker<T>(this, state, task,
                done,
                error);

        executorService.execute(tracker);

        return taskId;
    }

    private void end(long id) {
        // need to lock both to prevent transient state from leaking.
        synchronized (finished) {
            synchronized (pending) {
                final TaskState state = pending.remove(id);

                if (state == null) {
                    return;
                }

                finished.put(state.getId(), state.snapshot());
            }
        }
    }

    /* (non-Javadoc)
     * @see eu.toolchain.exposr.taskmanager.TaskManager#get(long)
     */
    @Override
    public TaskSnapshot get(long id) {
        final TaskSnapshot snapshot;

        synchronized (finished) {
            snapshot = this.finished.get(id);
        }

        if (snapshot != null) {
            return snapshot;
        }

        final TaskState state;

        synchronized (pending) {
            state = pending.get(id);
        }

        if (state == null) {
            return null;
        }

        return state.snapshot();
    }

    @Override
    public TaskSubscriber getSubscriber(long id) {
        final TaskState state;

        synchronized (pending) {
            state = pending.get(id);
        }

        if (state == null) {
            return null;
        }

        return state.subscriber();
    }

    /* (non-Javadoc)
     * @see eu.toolchain.exposr.taskmanager.TaskManager#getAll()
     */
    @Override
    public List<TaskSnapshot> getAll() {
        final List<TaskSnapshot> snapshots = new ArrayList<TaskSnapshot>();

        synchronized (finished) {
            snapshots.addAll(finished.values());
        }

        synchronized (pending) {
            for (TaskState state : pending.values()) {
                snapshots.add(state.snapshot());
            }
        }

        return snapshots;
    }

    public void stop() {
        executorService.shutdown();
    }
}
