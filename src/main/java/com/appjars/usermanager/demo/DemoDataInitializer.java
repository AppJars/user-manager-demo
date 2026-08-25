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

import com.appjars.usermanager.flow.util.RouteConfigurer;
import com.appjars.usermanager.model.AccessRuleDto;
import com.appjars.usermanager.model.AuthorityDto;
import com.appjars.usermanager.model.AuthorizationType;
import com.appjars.usermanager.model.GroupDto;
import com.appjars.usermanager.model.RuleType;
import com.appjars.usermanager.model.UserDto;
import com.appjars.usermanager.model.ViewSecurityDto;
import com.appjars.usermanager.service.AccessRuleService;
import com.appjars.usermanager.service.AuthorityService;
import com.appjars.usermanager.service.GroupService;
import com.appjars.usermanager.service.UserService;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the sample accounts, groups, roles and access rules the demo is presented with, so that an
 * evaluator lands on a populated application instead of empty screens.
 *
 * <p>The dataset is hardcoded and idempotent: each item is only created when it is missing, so it
 * survives restarts and never fights with changes made while exploring the demo. It stays well
 * below the free-license cap of five users, leaving room to create a couple more and watch the
 * limit kick in.
 *
 * <p>The AppJar itself only creates the {@code admin} account (see its default data generator);
 * everything below is demo content.
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(DemoDataInitializer.class);

  private static final String ROLE_ADMIN = "ADMIN";
  private static final String ROLE_SUPPORT = "SUPPORT";
  private static final String ROLE_VIEWER = "VIEWER";
  private static final String GROUP_SUPPORT = "Support team";

  private static final String RULE_ADMIN_ONLY =
      "Only administrators manage roles, access rules and the view matrix";
  private static final String RULE_SUPPORT_ONLY =
      "Only administrators and the support team reach the user and group screens";
  private static final String RULE_PROVIDERS =
      "Only administrators reach the external authentication screens";
  private static final String RULE_PROVIDERS_PATTERN = "um/auth-.*";
  private static final String RULE_PROFILE_ADMIN =
      "Disabled example: switch it on and only administrators keep their profile page";

  private final UserService userService;
  private final GroupService groupService;
  private final AuthorityService authorityService;
  private final AccessRuleService accessRuleService;
  private final PasswordEncoder passwordEncoder;

  @Value(RouteConfigurer.URL_USERS)
  private String usersUrl;

  @Value(RouteConfigurer.URL_GROUPS)
  private String groupsUrl;

  @Value(RouteConfigurer.URL_ROLES)
  private String rolesUrl;

  @Value(RouteConfigurer.URL_ACCESS_RULES)
  private String accessRulesUrl;

  @Value(RouteConfigurer.URL_VIEWS)
  private String viewsUrl;

  @Value(RouteConfigurer.URL_PROFILE)
  private String profileUrl;

  public DemoDataInitializer(
      UserService userService,
      GroupService groupService,
      AuthorityService authorityService,
      AccessRuleService accessRuleService,
      PasswordEncoder passwordEncoder) {
    this.userService = userService;
    this.groupService = groupService;
    this.authorityService = authorityService;
    this.accessRuleService = accessRuleService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      seed();
    } catch (RuntimeException e) {
      // Never keep the demo from starting because of its sample data (for instance when the free
      // user limit was already reached by accounts created while exploring)
      logger.warn("Demo sample data was not seeded completely: {}", e.getMessage());
    }
  }

  private void seed() {
    AuthorityDto admin = authority(ROLE_ADMIN);
    AuthorityDto support = authority(ROLE_SUPPORT);
    AuthorityDto viewer = authority(ROLE_VIEWER);

    GroupDto supportTeam = group(GROUP_SUPPORT, Set.of(support));

    user("maria", Set.of(), Set.of(supportTeam));
    user("diego", Set.of(viewer), Set.of());

    // Access is granted, never refused: a rule that matches decides on its own criteria, and what
    // the criteria does not grant is denied. So "viewers stay out" is expressed by naming the roles
    // that do get in, which is why VIEWER appears in neither rule.
    rule(RULE_ADMIN_ONLY, Set.of(rolesUrl, accessRulesUrl, viewsUrl), Set.of(admin));
    rule(RULE_SUPPORT_ONLY, Set.of(usersUrl, groupsUrl), Set.of(admin, support));

    // A pattern rule, so the demo shows the other matching type: it covers the auth provider
    // screens without naming them one by one, and would cover a new one starting with the same
    // prefix on its own.
    patternRule(RULE_PROVIDERS, RULE_PROVIDERS_PATTERN, Set.of(admin));

    // Kept disabled, which is how a rule is tried out without renumbering the rest: enable it from
    // the toggle in the grid and every account but an administrator loses the profile page.
    disabledRule(RULE_PROFILE_ADMIN, Set.of(profileUrl), Set.of(admin));
  }

  private AuthorityDto authority(String name) {
    return authorityService
        .findByName(name)
        .orElseGet(
            () -> {
              AuthorityDto authority = AuthorityDto.builder().name(name).build();
              authority.setId(authorityService.save(authority));
              logger.info("Demo data: created role '{}'", name);
              return authority;
            });
  }

  private GroupDto group(String name, Set<AuthorityDto> authorities) {
    return groupService
        .findByName(name)
        .orElseGet(
            () -> {
              GroupDto group = GroupDto.builder().name(name).authorities(authorities).build();
              group.setId(groupService.save(group));
              logger.info("Demo data: created group '{}'", name);
              return group;
            });
  }

  /** Creates a demo account whose password is its own username, as advertised on the landing. */
  private void user(String username, Set<AuthorityDto> authorities, Set<GroupDto> groups) {
    if (userService.findByUsername(username).isPresent()) {
      return;
    }
    UserDto user =
        UserDto.builder()
            .username(username)
            .encodedPassword(passwordEncoder.encode(username))
            .userAuthorities(new LinkedHashSet<>(authorities))
            .groups(new LinkedHashSet<>(groups))
            .enabled(true)
            .build();
    user.setId(userService.save(user));
    logger.info("Demo data: created user '{}'", username);
  }

  /**
   * Creates a rule granting the listed views to whoever holds any of the given roles. {@code
   * ANY_OF} rather than {@code ALL_OF} because the demo rules read as "these roles get in", not as
   * "hold every one of them"; with a single role the two are equivalent anyway.
   */
  private void rule(String description, Set<String> viewPaths, Set<AuthorityDto> grantedTo) {
    rule(description, viewPaths, grantedTo, true);
  }

  /** Same as {@link #rule}, but left switched off so the demo also shows a disabled rule. */
  private void disabledRule(String description, Set<String> viewPaths,
      Set<AuthorityDto> grantedTo) {
    rule(description, viewPaths, grantedTo, false);
  }

  private void rule(String description, Set<String> viewPaths, Set<AuthorityDto> grantedTo,
      boolean enabled) {
    if (ruleExists(description)) {
      return;
    }
    Set<ViewSecurityDto> views =
        viewPaths.stream()
            .sorted()
            .map(path -> ViewSecurityDto.builder().viewPath(path).build())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    AccessRuleDto rule =
        newRule(description, grantedTo, enabled)
            .type(RuleType.SIMPLE)
            .specificViews(views)
            .build();
    rule.setId(accessRuleService.save(rule));
    logger.info("Demo data: created access rule covering {}",
        Arrays.toString(viewPaths.toArray()));
  }

  /** Creates a rule that matches by regular expression rather than by a list of views. */
  private void patternRule(String description, String pattern, Set<AuthorityDto> grantedTo) {
    if (ruleExists(description)) {
      return;
    }
    AccessRuleDto rule =
        newRule(description, grantedTo, true)
            .type(RuleType.REGEX)
            .urlRegexPattern(pattern)
            .build();
    rule.setId(accessRuleService.save(rule));
    logger.info("Demo data: created access rule matching '{}'", pattern);
  }

  private boolean ruleExists(String description) {
    return accessRuleService.findAll().stream()
        .anyMatch(existing -> description.equals(existing.getDescription()));
  }

  private AccessRuleDto.AccessRuleDtoBuilder newRule(String description,
      Set<AuthorityDto> grantedTo, boolean enabled) {
    return AccessRuleDto.builder()
        .priority(accessRuleService.getLastPriority() + 1)
        .description(description)
        .enabled(enabled)
        .authorizationType(AuthorizationType.ANY_OF)
        .roles(new LinkedHashSet<>(grantedTo));
  }
}
