package eu.toolchain.exposr.taskmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toolchain.exposr.taskmanager.TaskSubscriber.Handle;

/**
 * Stores transient task state.
 * 
 * @author udoprog
 */
@Slf4j
@ToString(of = { "id", "title" })
public final class TaskState {
    private static final Logger ERR = LoggerFactory.getLogger("Task.ERR");
    private static final Logger OUT = LoggerFactory.getLogger("Task.OUT");
    private static final Logger SYS = LoggerFactory.getLogger("Task.SYS");

    @Getter
    private final long id;
    @Getter
    private final String title;

    private Date started;
    private Date ended;
    private Throwable error;

    private final Set<Handle> handlers = Collections
            .newSetFromMap(new ConcurrentHashMap<Handle, Boolean>());
    private final Queue<TaskOutput> output = new ConcurrentLinkedQueue<TaskOutput>();

    public TaskState(long id, String title) {
        this.id = id;
        this.title = title;
    }

    synchronized void start() {
        this.started = new Date();
    }

    void end() {
        end(null);
    }

    void end(Throwable error) {
        synchronized (this) {
            this.ended = new Date();
            this.error = error;
        }

        if (handlers.isEmpty()) {
            return;
        }

        for (Handle handle : handlers) {
            try {
                handle.close();
            } catch (Exception e) {
                log.error("Failed to call 'close' handle", e);
            }
        }

        handlers.clear();
    }

    public void system(String line) {
        SYS.info(id + ": " + line);
        write(new TaskOutput(TaskOutputType.SYS, line));
    }

    public void output(String line) {
        OUT.info(id + ": " + line);
        write(new TaskOutput(TaskOutputType.OUT, line));
    }

    public void error(String line) {
        ERR.info(id + ": " + line);
        write(new TaskOutput(TaskOutputType.ERR, line));
    }

    private void write(final TaskOutput out) {
        output.add(out);

        for (Handle handle : handlers) {
            try {
                handle.output(out);
            } catch (Exception e) {
                log.error("Failed to call 'line' handle", e);
                cleanup(handle);
            }
        }
    }

    public TaskSnapshot snapshot() {
        final List<TaskOutput> output = new ArrayList<TaskOutput>(this.output);

        synchronized (this) {
            return new TaskSnapshot(id, title, started, ended, error, output);
        }
    }

    public TaskSubscriber subscriber() {
        return new TaskSubscriber() {
            @Override
            public void subscribe(Handle handle) {
                handlers.add(handle);
            }
        };
    }

    private void cleanup(Handle handle) {
        try {
            handle.close();
        } catch (Exception e) {
            log.error("failed to 'close' handle", e);
        }

        handlers.remove(handle);
    }
}