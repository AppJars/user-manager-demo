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

package com.appjars.usermanager.demo.security;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

import com.appjars.usermanager.demo.views.LoginView;
import com.appjars.usermanager.flow.security.UserManagerNavigationAccessChecker;
import com.appjars.usermanager.service.util.RememberMeServicesProvider;
import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;

@ComponentScan(basePackageClasses = {SecurityConfiguration.class})
@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

  /** Route of {@link LoginView}, named here so Spring does not generate a login page of its own. */
  public static final String LOGIN_URL = "/login";

  public static final String LOGOUT_URL = "/";

  @Value("${com.appjars.usermanager.encoding.secret.key:1234567890}")
  private String secretKey;

  private final RememberMeServicesProvider rememberMeServicesProvider;

  public SecurityConfiguration(RememberMeServicesProvider rememberMeUtils) {
    this.rememberMeServicesProvider = rememberMeUtils;
  }

  @Bean
  static NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
    return new NavigationAccessControlConfigurer()
        .withAvailableNavigationAccessCheckers(
            checker -> checker instanceof UserManagerNavigationAccessChecker);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // Public paths must be declared BEFORE calling vaadin(),
    // as vaadin() adds a final anyRequest matcher.
    http.authorizeHttpRequests(
        authorize ->
            authorize
                .requestMatchers("/login*", "/offline-stub.html", "/offline-page.html")
                .permitAll()
                // VaadinSecurityConfigurer no longer permits these by default, and the anonymous
                // landing page needs them: /*.css covers the root styles.css loaded via @StyleSheet
                .requestMatchers(HttpMethod.GET, "/*.png", "/*.css", "/images/**", "/icons/**")
                .permitAll()
                .requestMatchers("/um/**")
                .authenticated());

    // The login page is the Vaadin LoginView, registered below by vaadin(). Naming it here as well
    // is what keeps Spring from generating one: a formLogin configurer with no loginPage leaves
    // DefaultLoginPageGeneratingFilter enabled, and that filter answers GET /login itself, so the
    // route never renders. Only the failure handler is ours; the rest of form login stays default.
    http.formLogin(configurer -> configurer
        .loginPage(LOGIN_URL)
        .permitAll()
        .failureHandler(authenticationFailureHandler()));

    http.rememberMe(
        rememberMeCustomizer ->
            rememberMeCustomizer
                .rememberMeServices(rememberMeServicesProvider.getRememberMeServices())
                .tokenValiditySeconds(7200));

    http.with(vaadin(), configurer -> configurer.loginView(LoginView.class, LOGOUT_URL));

    return http.build();
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers("/images/*.png");
  }

  public AuthenticationFailureHandler authenticationFailureHandler() {
    Map<String, String> exceptionMappings = new HashMap<>();
    exceptionMappings.put(DisabledException.class.getCanonicalName(), "/login?error=disabled");
    exceptionMappings.put(
        BadCredentialsException.class.getCanonicalName(), "/login?error=badcredentials");

    ExceptionMappingAuthenticationFailureHandler result =
        new ExceptionMappingAuthenticationFailureHandler();
    result.setExceptionMappings(exceptionMappings);
    result.setDefaultFailureUrl("/login?error");

    return result;
  }
}
