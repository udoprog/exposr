package eu.toolchain.exposr.taskmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskManager {
    private final ExecutorService executorService = Executors
            .newFixedThreadPool(10);

    private long id = 0;

    private final Map<Long, TaskState> pending = new ConcurrentHashMap<Long, TaskState>();
    private final Map<Long, TaskSnapshot> finished = new ConcurrentHashMap<Long, TaskSnapshot>();

    /*
     * (non-Javadoc)
     * 
     * @see eu.toolchain.exposr.taskmanager.TaskManager#build(java.lang.String,
     * eu.toolchain.exposr.taskmanager.Task)
     */
    public <T> SetupTask<T> build(String title, Task<T> task) {
        return new SetupTask<T>(this, title, task);
    }

    public <T> long execute(String title, Task<T> task,
            List<Task.Done<T>> done, List<Task.Error> error, Long parentId) {
        final long taskId;
        final TaskState state;

        synchronized (pending) {
            taskId = this.id++;
            state = new TaskState(taskId, title, parentId);
            pending.put(taskId, state);
        }

        if (log.isDebugEnabled())
            log.debug("Executing task [" + title + "] with id [" + taskId + "]");

        final TaskTracker<T> tracker = new TaskTracker<T>(this, state, task,
                done, error);

        executorService.execute(tracker);

        return taskId;
    }

    /**
     * End the task with the specific id.
     * 
     * @param id
     *            Id of the task to end.
     * @param snapshot
     *            Snapshot state of the ended task.
     */
    public void end(long id, final TaskSnapshot snapshot) {
        finished.put(id, snapshot);
        pending.remove(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.toolchain.exposr.taskmanager.TaskManager#get(long)
     */
    public TaskSnapshot get(long id) {
        final TaskSnapshot snapshot = this.finished.get(id);

        if (snapshot != null) {
            return snapshot;
        }

        final TaskState state = pending.get(id);

        if (state == null) {
            return null;
        }

        return state.snapshot();
    }

    public TaskSubscriber getSubscriber(long id) {
        final TaskState state = pending.get(id);

        if (state == null) {
            return null;
        }

        return state.subscriber();
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.toolchain.exposr.taskmanager.TaskManager#getAll()
     */
    public List<TaskSnapshot> getAll() {
        final List<TaskSnapshot> snapshots = new ArrayList<TaskSnapshot>(
                finished.values());

        for (TaskState state : pending.values()) {
            snapshots.add(state.snapshot());
        }

        return snapshots;
    }

    public void stop() {
        executorService.shutdown();
    }
}