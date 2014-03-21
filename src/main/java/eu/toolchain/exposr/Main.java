package eu.toolchain.exposr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.UriBuilder;

import lombok.extern.slf4j.Slf4j;

import org.glassfish.grizzly.http.server.HttpServer;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;

import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.http.EmbeddedGrizzly;
import eu.toolchain.exposr.project.InMemoryProjectReporter;
import eu.toolchain.exposr.project.LocalRepository;
import eu.toolchain.exposr.project.ProjectManager;
import eu.toolchain.exposr.project.ProjectReporter;
import eu.toolchain.exposr.publisher.Publisher;
import eu.toolchain.exposr.taskmanager.HandleBuilder;
import eu.toolchain.exposr.taskmanager.HandleBuilder.Handle;
import eu.toolchain.exposr.yaml.ExposrConfigYAML;
import eu.toolchain.exposr.yaml.ExposrConfigYAML.GithubProjectManagerYAML;
import eu.toolchain.exposr.yaml.ExposrConfigYAML.InMemoryProjectReporterYAML;
import eu.toolchain.exposr.yaml.ExposrConfigYAML.LocalBuilderYAML;
import eu.toolchain.exposr.yaml.ExposrConfigYAML.LocalPublisherYAML;

@Slf4j
public class Main extends GuiceServletContextListener {
    public static final String EXPOSR_CONFIG = "exposr.yaml";

    private static final class CustomConstructor extends Constructor {
        public CustomConstructor() {
            addTypeDescription(new TypeDescription(
                    GithubProjectManagerYAML.class,
                    GithubProjectManagerYAML.TYPE));
            addTypeDescription(new TypeDescription(
                    GithubProjectManagerYAML.BasicAuthYAML.class,
                    GithubProjectManagerYAML.BasicAuthYAML.TYPE));
            addTypeDescription(new TypeDescription(LocalPublisherYAML.class,
                    LocalPublisherYAML.TYPE));
            addTypeDescription(new TypeDescription(LocalBuilderYAML.class,
                    LocalBuilderYAML.TYPE));
            addTypeDescription(new TypeDescription(
                    InMemoryProjectReporterYAML.class,
                    InMemoryProjectReporterYAML.TYPE));
        }
    }

    public static Injector injector;
    private static ExposrConfigYAML config;
    private static final Object shutdownHook = new Object();

    @Override
    protected Injector getInjector() {
        log.info("Building Guice Injector");

        final List<Module> modules = new ArrayList<Module>();

        if (config.getProjectManager() == null) {
            throw new RuntimeException(
                    "No 'projectManager' specified in configuration");
        }

        final SchedulerModule.Config schedulerConfig = new SchedulerModule.Config();

        final ProjectManager projectManager = config.getProjectManager()
                .build();
        final LocalRepository localRepository = config.getRepository().build();
        final Publisher publisher = config.getPublisher().build();
        final Builder builder = config.getBuilder().build();
        final ProjectReporter projectReporter;

        if (config.getProjectReporter() == null) {
            projectReporter = new InMemoryProjectReporter();
        } else {
            projectReporter = config.getProjectReporter().build();
        }

        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(LocalRepository.class).toInstance(localRepository);
                bind(ProjectManager.class).toInstance(projectManager);
                bind(Publisher.class).toInstance(publisher);
                bind(Builder.class).toInstance(builder);
                bind(ProjectReporter.class).toInstance(projectReporter);
                bind(Object.class).annotatedWith(Names.named("shutdownHook"))
                        .toInstance(shutdownHook);
            }
        });
        modules.add(new SchedulerModule(schedulerConfig));
        modules.add(new ProjectModule(projectManager));
        modules.add(new TaskManagerModule());
        modules.add(new ProjectModule(projectManager));

        injector = Guice.createInjector(modules);

        final HandleBuilder<Void> refresh = projectManager.refresh();

        if (refresh != null) {
            refresh.callback(new Handle<Void>() {
                @Override
                public void done(Void value) {
                    localRepository.syncAll();
                }

                @Override
                public void error(Throwable t) {
                    log.error("Initial refresh failed", t);
                }
            }).execute();
        }

        return injector;
    }

    public static void main(String[] args) {
        final Yaml yaml = new Yaml(new CustomConstructor());

        final String configPath;

        if (args.length < 1) {
            configPath = EXPOSR_CONFIG;
        } else {
            configPath = args[0];
        }

        final File configFile = new File(configPath);

        try {
            config = yaml.loadAs(new FileInputStream(configFile),
                    ExposrConfigYAML.class);
        } catch (FileNotFoundException e) {
            log.error("Failed to read configuration", e);
            System.exit(1);
            return;
        }

        final EmbeddedGrizzly grizzlyServer = new EmbeddedGrizzly();
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