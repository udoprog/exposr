package eu.toolchain.exposr;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import eu.toolchain.exposr.taskmanager.DefaultTaskManager;
import eu.toolchain.exposr.taskmanager.TaskManager;

public class TaskManagerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(TaskManager.class).to(DefaultTaskManager.class).in(
                Scopes.SINGLETON);
    }
}