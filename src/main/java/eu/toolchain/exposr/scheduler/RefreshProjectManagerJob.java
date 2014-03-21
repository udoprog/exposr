package eu.toolchain.exposr.scheduler;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import eu.toolchain.exposr.project.ProjectManager;
import eu.toolchain.exposr.taskmanager.HandleBuilder;

@Slf4j
public class RefreshProjectManagerJob implements Job {
    @Inject
    private ProjectManager projectManager;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        log.info("Refreshing Project Manager");
        final HandleBuilder<Void> refresh = projectManager.refresh();
        
        if (refresh == null) {
            return;
        }

        refresh.execute();
    }
}