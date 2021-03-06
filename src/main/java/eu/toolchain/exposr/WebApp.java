package eu.toolchain.exposr;

import javax.inject.Inject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import com.google.inject.Injector;

import eu.toolchain.exposr.http.BuildResource;
import eu.toolchain.exposr.http.ExposrExceptionMapper;
import eu.toolchain.exposr.http.ExposrResource;
import eu.toolchain.exposr.http.ProjectResource;
import eu.toolchain.exposr.http.SyncResource;
import eu.toolchain.exposr.http.TaskResource;

@Slf4j
@RequiredArgsConstructor
public class WebApp extends ResourceConfig {
    @Inject
    public WebApp(Injector injector, ServiceLocator serviceLocator) {
        log.info("Setting up Web Application");

        register(SseFeature.class);
        register(MultiPartFeature.class);
        register(ExposrExceptionMapper.class);

        // Resources.
        register(BuildResource.class);
        register(ExposrResource.class);
        register(ProjectResource.class);
        register(SyncResource.class);
        register(TaskResource.class);

        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);

        final GuiceIntoHK2Bridge bridge = serviceLocator
                .getService(GuiceIntoHK2Bridge.class);

        bridge.bridgeGuiceInjector(injector);
    }
}