package eu.toolchain.exposr.taskmanager;

import eu.toolchain.exposr.taskmanager.HandleBuilder.Handle;
import eu.toolchain.exposr.taskmanager.HandleBuilder.OnDone;

public final class Handlers {
    /**
     * Create an adapter Callback between two different type signatues. The
     * result value provided to 'done' will always be null.
     * 
     * @param callback
     * @return The adapted Callback.
     */
    public static <T, R> Handle<T> adapter(final Handle<R> callback) {
        return new Handle<T>() {
            @Override
            public void done(TaskSnapshot task, T value) {
                callback.done(task, null);
            }

            @Override
            public void error(TaskSnapshot task, Throwable t) {
                callback.error(task, t);
            }
        };
    }

    /**
     * Create an adapter Callback between two different type signatues. The
     * result value provided to 'done' will always be null.
     * 
     * @param callback
     * @return The adapted Callback.
     */
    public static <T, R> OnDone<T> adapter(final OnDone<R> callback) {
        return new OnDone<T>() {
            @Override
            public void done(TaskSnapshot task, T value) {
                callback.done(task, null);
            }
        };
    }

    /**
     * Create an adapter CallbackBuilder between two different type signatures.
     * 
     * @param builder
     * @return An adapted CallbackBuilder.
     */
    public static <T, R> HandleBuilder<R> adapter(final HandleBuilder<T> builder) {
        return new HandleBuilder<R>() {
            @Override
            public HandleBuilder<R> callback(final Handle<R> callback) {
                builder.callback(Handlers.<T, R> adapter(callback));
                return this;
            }

            @Override
            public HandleBuilder<R> done(OnDone<R> callback) {
                builder.done(Handlers.<T, R> adapter(callback));
                return this;
            }

            @Override
            public HandleBuilder<R> error(OnError callback) {
                builder.error(callback);
                return this;
            }

            @Override
            public long execute() {
                return builder.execute();
            }
        };
    }
}
