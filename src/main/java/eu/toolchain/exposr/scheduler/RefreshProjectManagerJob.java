package eu.toolchain.exposr.scheduler;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.manager.RefreshableProjectManager;

@Slf4j
public class RefreshProjectManagerJob implements Job {
    @Inject
    private ProjectManager projectManager;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        if (!(projectManager instanceof RefreshableProjectManager)) {
            log.warn("Project manager does not support refresh: "
                    + projectManager);
            return;
        }

        final RefreshableProjectManager refreshable = RefreshableProjectManager.class
                .cast(projectManager);

        log.info("Refreshing Project Manager");
        refreshable.refresh().execute();
    }
}