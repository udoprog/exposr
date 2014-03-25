package eu.toolchain.exposr.taskmanager;

import java.util.List;

import eu.toolchain.exposr.taskmanager.TaskSetup.OnDone;
import eu.toolchain.exposr.taskmanager.TaskSetup.OnError;

public interface TaskManager {
    public <T> TaskSetup<T> build(String title, Task<T> task);

    public <T> long execute(String title, Task<T> task, List<OnDone<T>> done,
            List<OnError> error, Long parentId);

    public TaskSnapshot get(long id);

    public TaskSubscriber getSubscriber(long id);

    public List<TaskSnapshot> getAll();

    public void end(long id, final TaskSnapshot snapshot);
}