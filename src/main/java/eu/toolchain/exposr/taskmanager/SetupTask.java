package eu.toolchain.exposr.taskmanager;

import java.util.LinkedList;
import java.util.List;

import lombok.ToString;

@ToString(of = { "title", "task" })
public class SetupTask<T> {
    private final TaskManager manager;
    private final String title;
    private final Task<T> task;
    private final List<Task.Done<T>> done = new LinkedList<Task.Done<T>>();
    private final List<Task.Error> error = new LinkedList<Task.Error>();

    private Long parentId;

    public SetupTask(TaskManager manager, String title, Task<T> task) {
        this.manager = manager;
        this.title = title;
        this.task = task;
    }

    public SetupTask<T> callback(Task.Handle<T> callback) {
        done.add(callback);
        error.add(callback);
        return this;
    }

    public SetupTask<T> done(Task.Done<T> callback) {
        done.add(callback);
        return this;
    }

    public SetupTask<T> error(Task.Error callback) {
        error.add(callback);
        return this;
    }

    public SetupTask<T> parentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public long execute() {
        return manager.execute(title, task, done, error, parentId);
    }
}
