package eu.toolchain.exposr;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.UriBuilder;

import lombok.extern.slf4j.Slf4j;

import org.glassfish.grizzly.http.server.HttpServer;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;

import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.manager.RefreshableProjectManager;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.repository.Repository;
import eu.toolchain.exposr.taskmanager.DefaultTaskManager;
import eu.toolchain.exposr.taskmanager.HandleBuilder;
import eu.toolchain.exposr.taskmanager.HandleBuilder.Handle;
import eu.toolchain.exposr.taskmanager.TaskManager;
import eu.toolchain.exposr.taskmanager.TaskSnapshot;
import eu.toolchain.exposr.yaml.ExposrConfig;
import eu.toolchain.exposr.yaml.ValidationException;

@Slf4j
public class Main extends GuiceServletContextListener {
    public static final String EXPOSR_CONFIG = "exposr.yaml";

    public static Injector injector;
    private static ExposrConfig config;
    private static final Object shutdownHook = new Object();

    @Override
    protected Injector getInjector() {
        log.info("Building Guice Injector");

        final List<Module> modules = new ArrayList<Module>();

        final SchedulerModule.Config schedulerConfig = new SchedulerModule.Config();

        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Repository.class).toInstance(config.getRepository());
                bind(ProjectManager.class).toInstance(
                        config.getProjectManager());
                bind(Publisher.class).toInstance(config.getPublisher());
                bind(Builder.class).toInstance(config.getBuilder());
                bind(ProjectReporter.class).toInstance(
                        config.getProjectReporter());
                bind(Object.class).annotatedWith(Names.named("shutdownHook"))
                        .toInstance(shutdownHook);
                bind(TaskManager.class).to(DefaultTaskManager.class).in(
                        Scopes.SINGLETON);
            }
        });
        modules.add(new SchedulerModule(schedulerConfig, config
                .getProjectManager() instanceof RefreshableProjectManager));

        injector = Guice.createInjector(modules);

        final ProjectManager projectManager = config.getProjectManager();
        final Repository repository = config.getRepository();

        if (projectManager instanceof RefreshableProjectManager) {
            final HandleBuilder<Void> refresh = RefreshableProjectManager.class
                    .cast(projectManager).refresh();
    
            if (refresh != null) {
                refresh.callback(new Handle<Void>() {
                    @Override
                    public void done(TaskSnapshot task, Void value) {
                        repository.syncAll();
                    }
    
                    @Override
                    public void error(TaskSnapshot task, Throwable t) {
                        log.error("Initial refresh failed", t);
                    }
                }).execute();
            }
        } else {
            repository.syncAll();
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

        try {
            config = ExposrConfig.parse(Paths.get(configPath));
        } catch (ValidationException | IOException e) {
            log.error("Invalid configuration file: " + configPath);
            System.exit(1);
            return;
        }

        if (config == null) {
            log.error("No configuration, shutting down");
            System.exit(1);
            return;
        }

        final GrizzlyServer grizzlyServer = new GrizzlyServer();
        final HttpServer server;

        final Thread hook = new Thread(new Runnable() {
            @Override
            public void run() {
                log.warn("Shutdown Hook Invoked");

                synchronized (shutdownHook) {
                    shutdownHook.notifyAll();
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(hook);

        final URI baseUri = UriBuilder.fromUri("http://127.0.0.1/").port(8080)
                .build();

        synchronized (shutdownHook) {
            try {
                server = grizzlyServer.start(baseUri);
            } catch (IOException e) {
                log.error("Failed to start grizzly server", e);
                System.exit(1);
                return;
            }

            try {
                shutdownHook.wait();
            } catch (InterruptedException e) {
                log.error("Shutdown hook interrupted");
            }
        }

        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException e) {
            log.error("Could not remove shutdown hook", e);
        }

        final Scheduler scheduler = injector.getInstance(Scheduler.class);

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
}