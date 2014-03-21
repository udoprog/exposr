package eu.toolchain.exposr.taskmanager;

public interface Task<T> {
    public T run(TaskState state) throws Exception;
}
