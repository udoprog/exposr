package eu.toolchain.exposr.taskmanager;

/*
 * Subscribe to events on tasks.
 */
public interface TaskSubscriber {
    public static interface OnError {
        public void output(TaskOutput out) throws Exception;
    }

    public static interface OnClose {
        public void close() throws Exception;
    }

    public static interface Handle extends OnError, OnClose {
    }

    public void subscribe(Handle handle);
}
