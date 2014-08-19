package eu.toolchain.exposr;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.UriBuilder;

import lombok.extern.slf4j.Slf4j;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;

import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.manager.ProjectManagerRefreshed;
import eu.toolchain.exposr.project.manager.RefreshableProjectManager;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.repository.Repository;
import eu.toolchain.exposr.taskmanager.SetupTask;
import eu.toolchain.exposr.taskmanager.SetupTaskGroup;
import eu.toolchain.exposr.taskmanager.SetupTaskGroup.TaskError;
import eu.toolchain.exposr.taskmanager.SetupTaskGroup.TaskResult;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskManager;
import eu.toolchain.exposr.taskmanager.TaskSnapshot;
import eu.toolchain.exposr.tasks.SyncTaskResult;
import eu.toolchain.exposr.yaml.ExposrConfig;

@Slf4j
public class Main {
    public static final String EXPOSR_CONFIG = "exposr.yaml";

    private static Injector setupInjector(final ExposrConfig config,
            final CountDownLatch shutdown) {
        log.info("Building guice injector using config = " + config);

        final List<Module> modules = new ArrayList<Module>();

        final SchedulerModule.Config schedulerConfig = new SchedulerModule.Config();

        final RefreshableProjectManager refreshable;

        if (config.getProjectManager() instanceof RefreshableProjectManager) {
            refreshable = (RefreshableProjectManager) config
                    .getProjectManager();
        } else {
            refreshable = null;
        }

        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(CountDownLatch.class).annotatedWith(
                        Names.named("shutdown")).toInstance(shutdown);

                bind(Repository.class).toInstance(config.getRepository());
                bind(Publisher.class).toInstance(config.getPublisher());
                bind(Builder.class).toInstance(config.getBuilder());
                bind(ProjectReporter.class).toInstance(
                        config.getProjectReporter());
                bind(TaskManager.class).in(Scopes.SINGLETON);

                bind(ProjectManager.class).toInstance(
                        config.getProjectManager());
            }
        });

        modules.add(new SchedulerModule(schedulerConfig, refreshable != null));

        final Injector injector = Guice.createInjector(modules);

        final Repository repository = injector.getInstance(Repository.class);

        if (refreshable != null) {
            final SetupTask<ProjectManagerRefreshed> refresh = refreshable
                    .refresh();

            refresh.callback(new Task.Handle<ProjectManagerRefreshed>() {
                @Override
                public void done(TaskSnapshot task,
                        ProjectManagerRefreshed value) {
                    repository
                            .syncAll()
                            .parentId(task.getParentId())
                            .callback(
                                    new SetupTaskGroup.Handle<SyncTaskResult>() {
                                        @Override
                                        public void done(
                                                Collection<TaskResult<SyncTaskResult>> results,
                                                Collection<TaskError> errors) {
                                            log.info("Everything synchronized: "
                                                    + results + ":" + errors);
                                        }
                                    }).execute();
                }

                @Override
                public void error(TaskSnapshot task, Throwable t) {
                    log.error("Initial refresh failed", t);
                }
            });

            refresh.execute();
        } else {
            repository.syncAll().execute();
        }

        return injector;
    }

    public static void main(String[] args) {
        final String configPath;

        if (args.length < 1) {
            configPath = EXPOSR_CONFIG;
        } else {
            configPath = args[0];
        }

        final ExposrConfig config;

        try {
            config = ExposrConfig.parse(Paths.get(configPath));
        } catch (Exception e) {
            log.error("Invalid configuration file: " + configPath, e);
            System.exit(1);
            return;
        }

        if (config == null) {
            log.error("No configuration, shutting down");
            System.exit(1);
            return;
        }

        final URI baseUri = UriBuilder.fromUri("http://127.0.0.1/").port(8080)
                .build();


        final CountDownLatch shutdown = new CountDownLatch(1);

        final Injector injector = setupInjector(config, shutdown);
        final HttpServer server = setupHttpServer(baseUri, injector);

        final Scheduler scheduler = injector.getInstance(Scheduler.class);

        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start grizzly server", e);
            System.exit(1);
            return;
        }

        try {
            shutdown.await();
        } catch (InterruptedException e) {
            log.error("Shutdown hook interrupted", e);
        }

        log.warn("Shutting down scheduler");

        try {
            scheduler.shutdown(true);
        } catch (SchedulerException e) {
            log.error("Scheduler shutdown failed", e);
        }

        try {
            log.warn("Waiting for server to shutdown");
            server.shutdown().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Server shutdown failed", e);
        }

        log.warn("Bye Bye!");
        System.exit(0);
    }

    private static HttpServer setupHttpServer(URI baseUri, final Injector injector) {
        final HttpServer serverLocal = GrizzlyHttpServerFactory
                .createHttpServer(baseUri, false);

        final WebappContext context = new WebappContext("Guice Webapp sample",
                "");

        context.addListener(new GuiceServletContextListener(){
            @Override
            protected Injector getInjector() {
                return injector;
            }
        });

        // Initialize and register Jersey ServletContainer
        final ServletRegistration servletRegistration = context.addServlet(
                "ServletContainer", ServletContainer.class);
        servletRegistration.addMapping("/*");
        servletRegistration.setInitParameter("javax.ws.rs.Application",
                WebApp.class.getName());

        // Initialize and register GuiceFilter
        final FilterRegistration registration = context.addFilter(
                "GuiceFilter", GuiceFilter.class);
        registration.addMappingForUrlPatterns(
                EnumSet.allOf(DispatcherType.class), "/*");

        context.deploy(serverLocal);

        return serverLocal;
    }
}