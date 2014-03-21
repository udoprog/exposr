package eu.toolchain.exposr.taskmanager;

import java.util.List;

public interface TaskManager {
    public <T> HandleBuilder<T> build(String title, Task<T> task);

    public TaskSnapshot get(long id);

    public TaskSubscriber getSubscriber(long id);

    public List<TaskSnapshot> getAll();

}