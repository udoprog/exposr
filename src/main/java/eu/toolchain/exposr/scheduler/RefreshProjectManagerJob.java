package eu.toolchain.exposr.scheduler;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import eu.toolchain.exposr.project.manager.RefreshableProjectManager;

@Slf4j
public class RefreshProjectManagerJob implements Job {
    @Inject
    private RefreshableProjectManager projectManager;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        log.info("Refreshing Project Manager");
        projectManager.refresh().execute();
    }
}