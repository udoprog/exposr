package eu.toolchain.exposr.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.inject.Inject;

import eu.toolchain.exposr.repository.Repository;

public class SyncRemotesJob implements Job {
    @Inject
    private Repository localRepository;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        localRepository.syncAll();
    }
}