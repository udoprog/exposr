package eu.toolchain.exposr.taskmanager;

public interface HandleBuilder<T> {
    public static interface OnDone<T> {
        void done(TaskSnapshot task, T value);
    }

    public static interface OnError {
        void error(TaskSnapshot task, Throwable t);
    }

    public static interface Handle<T> extends OnDone<T>, OnError {
    }

    public HandleBuilder<T> callback(Handle<T> handle);

    public HandleBuilder<T> done(OnDone<T> done);

    public HandleBuilder<T> error(OnError error);

    public long execute();
}
