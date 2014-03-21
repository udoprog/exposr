package eu.toolchain.exposr;

import lombok.extern.slf4j.Slf4j;

import org.apache.onami.scheduler.QuartzModule;
import org.quartz.JobKey;

import eu.toolchain.exposr.scheduler.RefreshProjectManagerJob;
import eu.toolchain.exposr.scheduler.SyncRemotesJob;

@Slf4j
public class SchedulerModule extends QuartzModule {
    public static final JobKey REFRESH = JobKey.jobKey("refresh");
    public static final JobKey LS_REMOTE = JobKey.jobKey("lsRemote");

    public static final class Config {
        private String syncRemotesSchedule = "0 0 * * * ?";
        private String refreshProjectManagerSchedule = "0 0/15 * * * ?";

        public String getSyncRemoteSchedule() {
            return syncRemotesSchedule;
        }

        public void setSyncRemoteSchedule(String lsRemoteSchedule) {
            this.syncRemotesSchedule = lsRemoteSchedule;
        }

        public String getRefreshProjectManagerSchedule() {
            return refreshProjectManagerSchedule;
        }

        public void setRefreshProjectManagerSchedule(String refreshSchedule) {
            this.refreshProjectManagerSchedule = refreshSchedule;
        }
    }

    private final Config config;
    private final boolean refreshable;

    public SchedulerModule(final Config config, final boolean refreshable) {
        this.config = config;
        this.refreshable = refreshable;
    }

    @Override
    protected void schedule() {
        log.info("Setting up schedule for remote syncing");
        scheduleJob(SyncRemotesJob.class).withCronExpression(
                config.getSyncRemoteSchedule())
                .withJobName(LS_REMOTE.getName());

        if (refreshable) {
            log.info("Setting up schedule for ProjectManager");
            scheduleJob(RefreshProjectManagerJob.class).withCronExpression(
                    config.getRefreshProjectManagerSchedule()).withJobName(
                    REFRESH.getName());
        }
    }
}
