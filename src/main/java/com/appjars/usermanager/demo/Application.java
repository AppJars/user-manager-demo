/*-
 * #%L
 * User Manager AppJars - Demo
 * %%
 * Copyright (C) 2023 - 2026 AppJars
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.appjars.usermanager.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.appjars.AppJarsAutoConfiguration;
import com.appjars.usermanager.UserManagerAutoConfiguration;
import com.appjars.usermanager.demo.views.MainLayout;
import com.appjars.usermanager.flow.util.RouteConfigurer;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.lumo.Lumo;

import jakarta.annotation.PostConstruct;

/**
 * The entry point of the Spring Boot application.
 *
 * <p>Use the @PWA annotation make the application installable on phones, tablets and some desktop
 * browsers.
 */
@SuppressWarnings("serial")
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
@PWA(
    name = "User Manager Demo",
    shortName = "User Manager Demo",
    offlineResources = {})
@EnableVaadin({
  "com.appjars.usermanager.demo",
  "com.appjars.usermanager.vaadin",
})
@EnableJpaRepositories(basePackageClasses = UserManagerAutoConfiguration.class)
@ComponentScan(
    basePackageClasses = {UserManagerAutoConfiguration.class, AppJarsAutoConfiguration.class})
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

  private final RouteConfigurer routeConfigurer;

  public Application(RouteConfigurer routeConfigurer) {
    this.routeConfigurer = routeConfigurer;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void configurePage(AppShellSettings settings) {
    settings.addFavIcon("icon", "icons/icon.png", "180x180");
  }

  @PostConstruct
  public void configure() {
    routeConfigurer.setViewsRouterLayout(MainLayout.class);
  }
}
