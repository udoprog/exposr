package eu.toolchain.exposr;

import lombok.Getter;

import org.apache.onami.scheduler.QuartzModule;
import org.quartz.JobKey;

import eu.toolchain.exposr.scheduler.RefreshProjectManagerJob;
import eu.toolchain.exposr.scheduler.SyncRemotesJob;

public class SchedulerModule extends QuartzModule {
    public static final JobKey REFRESH = JobKey.jobKey("refresh");
    public static final JobKey LS_REMOTE = JobKey.jobKey("lsRemote");

    public static final class Config {
        @Getter
        private final String syncRemotesSchedule = "0 0 * * * ?";

        @Getter
        private final String refreshProjectManagerSchedule = "0 0/15 * * * ?";
    }

    private final Config config;
    private final boolean refreshable;

    public SchedulerModule(Config config, boolean refreshable) {
        this.config = config;
        this.refreshable = refreshable;
    }

    @Override
    protected void schedule() {
        scheduleJob(SyncRemotesJob.class).withCronExpression(
                config.getSyncRemotesSchedule()).withJobName(
                LS_REMOTE.getName());

        if (refreshable) {
            scheduleJob(RefreshProjectManagerJob.class).withCronExpression(
                    config.getRefreshProjectManagerSchedule()).withJobName(
                    REFRESH.getName());
        }
    }
}
