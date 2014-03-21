package eu.toolchain.exposr.project.reporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.toolchain.exposr.project.Project;

public class MemoryProjectLog<T> {
    private final Map<Project, List<T>> items = new HashMap<Project, List<T>>();

    public synchronized void append(Project project, T item) {
        List<T> log = items.get(project);

        if (log == null) {
            log = new ArrayList<T>();
            items.put(project, log);
        }

        log.add(item);
    }

    public synchronized T getLast(Project project) {
        final List<T> log = items.get(project);

        if (log == null) {
            return null;
        }

        if (log.isEmpty()) {
            return null;
        }

        return log.get(log.size() - 1);
    }

    public synchronized List<T> all(Project project) {
        final List<T> log = items.get(project);

        if (log == null) {
            return null;
        }

        return new ArrayList<T>(log);
    }
}
