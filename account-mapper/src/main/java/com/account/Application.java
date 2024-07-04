package com.account;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class Application implements WebMvcConfigurer {
   /**
    * The entry point of the Spring Boot Application.
    * The Application can be run in two modes:
    * </p>
    * * Web Interface - The default behaviour of the application. A web server
    * is initialized on the specified ${server.port}.
    * </p>
    * * Command Line Interface - when the `--command.line.interface.enabled=true`
    * is passed, a Tomcat Web Server is not initialized and the program is run
    * as a standalone command line program instead.
    * </p>
    *
    * @param args - The program arguments
    */
   public static void main(String[] args) {
      final SpringApplication app = new SpringApplication(Application.class);
      //@formatter:off
      final boolean cmdLineMode = Boolean.parseBoolean(Arrays.stream(args)
            .filter(arg -> arg.startsWith("--command.line.interface.enabled="))
            .findFirst()
            .orElse("--command.line.interface.enabled=false")
            .split("=")[1]
      );
      //@formatter:on
      app.setWebApplicationType(
            cmdLineMode ? WebApplicationType.NONE : WebApplicationType.SERVLET);
      app.run(args);
   }
}
