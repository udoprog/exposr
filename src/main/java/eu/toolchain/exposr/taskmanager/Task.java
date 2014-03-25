package eu.toolchain.exposr.taskmanager;

public interface Task<T> {
    public static interface Done<T> {
        void done(TaskSnapshot task, T value);
    }

    public static interface Error {
        void error(TaskSnapshot task, Throwable t);
    }

    public static interface Handle<T> extends Done<T>, Error {
    }

    public T run(TaskState state) throws Exception;
}
