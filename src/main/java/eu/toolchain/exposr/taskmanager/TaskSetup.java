package eu.toolchain.exposr.taskmanager;

import java.util.LinkedList;
import java.util.List;

public class TaskSetup<T> {
    public static interface OnDone<T> {
        void done(TaskSnapshot task, T value);
    }

    public static interface OnError {
        void error(TaskSnapshot task, Throwable t);
    }

    public static interface Handle<T> extends OnDone<T>, OnError {
    }

    private final DefaultTaskManager manager;
    private final String title;
    private final Task<T> task;
    private final List<OnDone<T>> done = new LinkedList<OnDone<T>>();
    private final List<OnError> error = new LinkedList<OnError>();

    private Long parentId;

    public TaskSetup(DefaultTaskManager manager, String title, Task<T> task) {
        this.manager = manager;
        this.title = title;
        this.task = task;
    }

    public TaskSetup<T> callback(Handle<T> callback) {
        done.add(callback);
        error.add(callback);
        return this;
    }

    public TaskSetup<T> done(OnDone<T> callback) {
        done.add(callback);
        return this;
    }

    public TaskSetup<T> error(OnError callback) {
        error.add(callback);
        return this;
    }

    public TaskSetup<T> parentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public long execute() {
        return manager.execute(title, task, done, error, parentId);
    }
}
