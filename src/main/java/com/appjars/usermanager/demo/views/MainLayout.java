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

package com.appjars.usermanager.demo.views;

import com.appjars.usermanager.demo.views.tour.DemoTours;
import com.appjars.usermanager.demo.views.tour.DemoTours.DemoTour;
import com.appjars.usermanager.flow.util.AccessControlHelper;
import com.appjars.usermanager.flow.util.AccessControlHelper.RoleViewAccess;
import com.appjars.usermanager.flow.util.AccessControlHelper.ViewAccessStatus;
import com.appjars.usermanager.flow.util.UserSessionUtils;
import com.appjars.usermanager.flow.view.AuthProvidersListView;
import com.appjars.usermanager.flow.view.AuthoritiesView;
import com.appjars.usermanager.flow.view.GroupsListView;
import com.appjars.usermanager.flow.view.ProfileView;
import com.appjars.usermanager.flow.view.RulesView;
import com.appjars.usermanager.flow.view.UsersListView;
import com.appjars.usermanager.flow.view.ViewsView;
import com.appjars.usermanager.model.AuthorityDto;
import com.appjars.usermanager.model.UserDto;
import com.appjars.usermanager.service.UserService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.security.Principal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main view is a top-level placeholder for other views.
 *
 * <p>The layout is anonymous so that the public landing page ({@link HomeView}) can render inside
 * it; every other view keeps enforcing its own {@code @PermitAll} and {@link #beforeEnter} still
 * reroutes anonymous visitors to the login view. In Vaadin 25 the access checker denies a view
 * whose parent layout is annotated more strictly than the view itself, so a {@code @PermitAll}
 * layout would block the anonymous landing page.
 */
@SuppressWarnings("serial")
@AnonymousAllowed
public class MainLayout extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

  private final UserService userService;
  private final UserSessionUtils userSessionProvider;
  private final AccessAnnotationChecker accessChecker;
  private final AccessControlHelper accessControlHelper;

  /** Resolved once per layout, since both the drawer and the tour menu are built from it. */
  private final Optional<Set<String>> reachableViews;

  private H2 viewTitle;

  public MainLayout(
      UserService userService,
      UserSessionUtils userSessionProvider,
      AccessAnnotationChecker accessAnnotationChecker,
      AccessControlHelper accessControlHelper) {

    this.userService = userService;
    this.userSessionProvider = userSessionProvider;
    this.accessChecker = accessAnnotationChecker;
    this.accessControlHelper = accessControlHelper;
    this.reachableViews = resolveReachableViews();

    setPrimarySection(Section.DRAWER);
    addDrawerContent();
    addHeaderContent();
  }

  /**
   * The views the signed-in account can actually open, by simple class name, or empty for an
   * anonymous visitor.
   *
   * <p>{@link AccessAnnotationChecker} only reads a view's annotations, and every appjar view
   * grants those to any authenticated user, so on its own it lets the drawer and the tour menu
   * offer screens that the access rules then refuse on navigation. {@link AccessControlHelper}
   * answers with the same terminal rule semantics navigation uses, which is what makes the two
   * lists agree with what actually happens on click.
   *
   * <p>It models its argument as an authenticated user, so it cannot speak for an anonymous
   * visitor. That case returns empty and the annotation check decides on its own, which is
   * already the whole truth there: the views deny anonymous by annotation, before any rule.
   */
  private Optional<Set<String>> resolveReachableViews() {
    Principal principal = VaadinRequest.getCurrent().getUserPrincipal();
    if (principal == null) {
      return Optional.empty();
    }
    Set<AuthorityDto> authorities = userService.findByUsername(principal.getName())
        .map(UserDto::getAllAuthorities)
        .orElseGet(Set::of);
    return Optional.of(accessControlHelper.resolveViewAccess(authorities).stream()
        // Conditional means the outcome depends on concrete parameter values, so it is not a
        // refusal and the view stays offered.
        .filter(access -> access.status() != ViewAccessStatus.NOT_ACCESSIBLE)
        .map(RoleViewAccess::viewName)
        .collect(Collectors.toSet()));
  }

  /** Whether the current visitor can open a view, by its annotations and by the access rules. */
  private boolean canReach(Class<? extends Component> view) {
    return accessChecker.hasAccess(view)
        && reachableViews.map(names -> names.contains(view.getSimpleName())).orElse(true);
  }

  private void addHeaderContent() {
    DrawerToggle toggle = new DrawerToggle();
    toggle.setAriaLabel(getTranslation("appjars.usermanager.demo.layout.menutoggle"));

    viewTitle = new H2();
    viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

    addToNavbar(toggle, viewTitle, createTourMenu());
  }

  private MenuBar createTourMenu() {
    MenuBar menu = new MenuBar();
    menu.addClassName("navbar-tour-menu");
    menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    SubMenu tours = menu
        .addItem(new Div(VaadinIcon.MAP_MARKER.create(),
            new Span(getTranslation("appjars.usermanager.demo.tour.button"))))
        .getSubMenu();
    tours.addItem(getTranslation("appjars.usermanager.demo.tour.thispage"),
        e -> startCurrentTour());
    for (DemoTour tour : DemoTour.values()) {
      MenuItem item = tours.addItem(getTranslation(DemoTours.labelKey(tour)), e -> startTour(tour));
      // The catalogue stays complete on every account, because each label already names the
      // accounts its tour needs; disabling turns that hint into something the visitor cannot walk
      // past into an access-denied page.
      Class<? extends Component> view = tourView(tour);
      item.setEnabled(view == null || canReach(view));
    }
    return menu;
  }

  private void startTour(DemoTour tour) {
    Class<? extends Component> target = tourView(tour);
    if (target == null || (getContent() != null && target.equals(getContent().getClass()))) {
      DemoTours.start(tour, this, this::getTranslation);
    } else {
      VaadinSession.getCurrent().setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, tour);
      getUI().ifPresent(ui -> ui.navigate(target));
    }
  }

  /**
   * Starts the tour of the view being shown. Not every route has one - the account and group
   * editors and the link screens are reachable without being a tour's home - so when there is none
   * the visitor is told, rather than left with a menu entry that silently does nothing.
   */
  private void startCurrentTour() {
    Class<?> current = getContent() == null ? null : getContent().getClass();
    for (DemoTour tour : DemoTour.values()) {
      if (current != null && current.equals(tourView(tour))) {
        DemoTours.start(tour, this, this::getTranslation);
        return;
      }
    }
    Notification.show(getTranslation("appjars.usermanager.demo.tour.none"));
  }

  private Class<? extends Component> tourView(DemoTour tour) {
    return switch (tour) {
      case HOME -> HomeView.class;
      case NAVIGATION -> null;
      case PROFILE -> ProfileView.class;
      case USERS -> UsersListView.class;
      case GROUPS -> GroupsListView.class;
      case ROLES -> AuthoritiesView.class;
      case RULES -> RulesView.class;
      case VIEWS -> ViewsView.class;
      case PROVIDERS -> AuthProvidersListView.class;
    };
  }

  private void addDrawerContent() {
    H1 appName = new H1(getTranslation("appjars.usermanager.demo.layout.drawertitle"));
    appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
    Header header = new Header(appName);
    header.getStyle().set("padding", "var(--lumo-space-m)");

    Scroller scroller = new Scroller(createNavigation());

    Footer footer = createFooter();
    footer.getStyle().set("padding", "var(--lumo-space-s)");

    VerticalLayout drawerContainer = new VerticalLayout(header, scroller, footer);
    drawerContainer.getStyle().set("position", "relative");
    drawerContainer.setSizeFull();
    drawerContainer.setAlignItems(Alignment.STRETCH);
    drawerContainer.getStyle().set("overflow", "hidden");
    drawerContainer.setPadding(false);
    drawerContainer.setSpacing(false);
    drawerContainer.setFlexGrow(1, scroller);

    addToDrawer(drawerContainer);
  }

  private SideNav createNavigation() {
    SideNav nav = new SideNav();

    SideNavItem homeItem =
        new SideNavItem(getTranslation("appjars.usermanager.demo.menuitem.home"), HomeView.class);
    homeItem.setPrefixComponent(VaadinIcon.HOME.create());
    homeItem.setId("nav-home");
    nav.addItem(homeItem);

    SideNavItem accessRulesItem =
        new SideNavItem(getTranslation("appjars.usermanager.demo.menuitem.accessrules"));
    accessRulesItem.setExpanded(true);
    accessRulesItem.setId("nav-accessrules");
    if (canReach(RulesView.class)) {
      accessRulesItem.addItem(
          navItem("nav-rules", "appjars.usermanager.demo.menuitem.rules", RulesView.class));
    }
    if (canReach(ViewsView.class)) {
      accessRulesItem.addItem(
          navItem("nav-views", "appjars.usermanager.demo.menuitem.views", ViewsView.class));
    }

    SideNavItem userManagerItem =
        new SideNavItem(getTranslation("appjars.usermanager.demo.menuitem.usermanager"));
    userManagerItem.setPrefixComponent(VaadinIcon.USER.create());
    userManagerItem.setExpanded(true);
    userManagerItem.setId("nav-usermanager");

    if (canReach(ProfileView.class)) {
      userManagerItem.addItem(
          navItem("nav-profile", "appjars.usermanager.demo.menuitem.profile", ProfileView.class));
    }
    if (canReach(UsersListView.class)) {
      userManagerItem.addItem(
          navItem("nav-users", "appjars.usermanager.demo.menuitem.users", UsersListView.class));
    }
    if (canReach(GroupsListView.class)) {
      userManagerItem.addItem(
          navItem("nav-groups", "appjars.usermanager.demo.menuitem.groups", GroupsListView.class));
    }
    if (canReach(AuthoritiesView.class)) {
      userManagerItem.addItem(
          navItem("nav-roles", "appjars.usermanager.demo.menuitem.authorities",
              AuthoritiesView.class));
    }
    if (canReach(AuthProvidersListView.class)) {
      userManagerItem.addItem(
          navItem("nav-providers", "appjars.usermanager.demo.menuitem.providers",
              AuthProvidersListView.class));
    }
    if (canReach(RulesView.class) || canReach(ViewsView.class)) {
      userManagerItem.addItem(accessRulesItem);
    }

    // Anonymous visitors have access to none of these, and an empty parent item is just noise
    if (!userManagerItem.getItems().isEmpty()) {
      nav.addItem(userManagerItem);
    }

    return nav;
  }

  private SideNavItem navItem(String id, String labelKey, Class<? extends Component> view) {
    SideNavItem item = new SideNavItem(getTranslation(labelKey), view);
    item.setId(id);
    return item;
  }

  private Footer createFooter() {
    Footer layout = new Footer();

    Optional<Principal> userOpt =
        Optional.ofNullable(VaadinRequest.getCurrent().getUserPrincipal());

    if (userOpt.isPresent()) {
      UserDto user =
          userService
              .findByUsername(userOpt.get().getName())
              .orElseThrow(() -> new IllegalStateException("Logged user could not be found."));

      Avatar avatar = new Avatar(user.getUsername());
      avatar.setThemeName("xsmall");
      avatar.getElement().setAttribute("tabindex", "-1");

      MenuBar userMenu = new MenuBar();
      userMenu.setThemeName("tertiary-inline contrast");

      MenuItem userName = userMenu.addItem("");
      Div div = new Div();
      div.add(avatar);
      div.add(user.getUsername());
      div.add(LumoIcon.DROPDOWN.create());
      div.getElement().getStyle().set("display", "flex");
      div.getElement().getStyle().set("align-items", "center");
      div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
      userName.add(div);
      userName
          .getSubMenu()
          .addItem(
              getTranslation("appjars.usermanager.demo.layout.signout"),
              e -> {
                userSessionProvider.logout();
              });

      layout.add(userMenu);
    } else {
      Anchor loginLink =
          new Anchor("login", getTranslation("appjars.usermanager.demo.layout.signin"));
      layout.add(loginLink);
    }

    return layout;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    if (VaadinRequest.getCurrent().getUserPrincipal() == null
        && !HomeView.class.equals(beforeEnterEvent.getNavigationTarget())) {
      beforeEnterEvent.rerouteTo(LoginView.class);
    }
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    viewTitle.setText(getCurrentPageTitle());
    startPendingTour();
  }

  private void startPendingTour() {
    VaadinSession session = VaadinSession.getCurrent();
    if (session.getAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE) instanceof DemoTour pending
        && getContent() != null
        && getContent().getClass().equals(tourView(pending))) {
      session.setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, null);
      DemoTours.start(pending, this, this::getTranslation);
    }
  }

  private String getCurrentPageTitle() {
    if (getContent() instanceof HasDynamicTitle dynamicTitle) {
      return dynamicTitle.getPageTitle();
    }
    PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
    return title == null ? "" : title.value();
  }
}
