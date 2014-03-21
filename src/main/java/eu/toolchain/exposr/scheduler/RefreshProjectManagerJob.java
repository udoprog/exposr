package eu.toolchain.exposr.scheduler;

import lombok.extern.slf4j.Slf4j;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.inject.Inject;
import eu.toolchain.exposr.project.ProjectManager;

@Slf4j
public class RefreshProjectManagerJob implements Job {
    @Inject
    private ProjectManager projectManager;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        log.info("Refreshing Project Manager");
        projectManager.refresh().execute();
    }
}