package eu.toolchain.exposr.http;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import com.google.inject.Injector;
import eu.toolchain.exposr.Main;

@Slf4j
public class WebApplication extends ResourceConfig {
    @Inject
    public WebApplication(ServiceLocator serviceLocator) {
        log.info("Registering injectables...");

        register(SseFeature.class);
        
        // Set package to look for resources in
        packages("eu.toolchain.exposr.http");

        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);

        final GuiceIntoHK2Bridge bridge = serviceLocator
                .getService(GuiceIntoHK2Bridge.class);

        final Injector injector = Main.injector;

        bridge.bridgeGuiceInjector(injector);
    }
}