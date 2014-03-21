package eu.toolchain.exposr.http;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import lombok.extern.slf4j.Slf4j;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.inject.servlet.GuiceFilter;
import eu.toolchain.exposr.Main;

@Slf4j
public class EmbeddedGrizzly {
    private HttpServer server;

    public HttpServer start(URI baseUri) throws IOException {
        log.info("Starting grizzly...");

        final HttpServer serverLocal = GrizzlyHttpServerFactory
                .createHttpServer(baseUri, false);

        final WebappContext context = new WebappContext("Guice Webapp sample",
                "");

        context.addListener(new Main());

        // Initialize and register Jersey ServletContainer
        final ServletRegistration servletRegistration = context.addServlet(
                "ServletContainer", ServletContainer.class);
        servletRegistration.addMapping("/*");
        servletRegistration.setInitParameter("javax.ws.rs.Application",
                "eu.toolchain.exposr.http.WebApplication");

        // Initialize and register GuiceFilter
        final FilterRegistration registration = context.addFilter(
                "GuiceFilter", GuiceFilter.class);
        registration.addMappingForUrlPatterns(
                EnumSet.allOf(DispatcherType.class), "/*");

        context.deploy(serverLocal);

        serverLocal.start();

        server = serverLocal;
        return server;
    }
}