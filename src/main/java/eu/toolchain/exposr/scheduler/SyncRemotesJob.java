package eu.toolchain.exposr.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.inject.Inject;
import eu.toolchain.exposr.project.LocalRepository;

public class SyncRemotesJob implements Job {
    @Inject
    private LocalRepository localRepository;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        localRepository.syncAll();
    }
}