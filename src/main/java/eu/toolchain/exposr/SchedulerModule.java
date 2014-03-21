package eu.toolchain.exposr;

import org.apache.onami.scheduler.QuartzModule;
import org.quartz.JobKey;

import eu.toolchain.exposr.scheduler.RefreshProjectManagerJob;
import eu.toolchain.exposr.scheduler.SyncRemotesJob;

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

    public SchedulerModule(final Config config) {
        this.config = config;
    }

    @Override
    protected void schedule() {
        scheduleJob(SyncRemotesJob.class).withCronExpression(
                config.getSyncRemoteSchedule())
                .withJobName(LS_REMOTE.getName());

        scheduleJob(RefreshProjectManagerJob.class).withCronExpression(
                config.getRefreshProjectManagerSchedule()).withJobName(
                REFRESH.getName());
    }
}
