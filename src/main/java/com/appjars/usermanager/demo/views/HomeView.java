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
import com.appjars.usermanager.flow.view.AuthProvidersListView;
import com.appjars.usermanager.flow.view.AuthoritiesView;
import com.appjars.usermanager.flow.view.GroupsListView;
import com.appjars.usermanager.flow.view.ProfileView;
import com.appjars.usermanager.flow.view.RulesView;
import com.appjars.usermanager.flow.view.UsersListView;
import com.appjars.usermanager.flow.view.ViewsView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Public landing page of the demo: presents the appjar features, the demo credentials, the license
 * model and offers guided tours of the views.
 */
@SuppressWarnings("serial")
@AnonymousAllowed
@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout implements HasDynamicTitle {

  private static final String KEY_PREFIX = "appjars.usermanager.demo.home.";

  private static final String APPJARS_SITE_URL = "https://www.appjars.com";
  private static final String GITHUB_ORG_URL = "https://github.com/AppJars";
  private static final String DOCS_URL = "https://docs.appjars.com/user-manager/overview/";
  private static final String LOGO_PATH = "icons/icon-appjars-full.png";

  public HomeView() {
    addClassName("home-view");
    add(createHero(), createFeaturesSection(), createTryItSection(), createLicenseSection(),
        createLinksSection());
    setAlignItems(Alignment.STRETCH);
  }

  private Component createHero() {
    Image logo = new Image(LOGO_PATH, t("hero.logoAlt"));
    logo.addClassName("home-hero-logo");
    logo.setWidth("144px");
    logo.setHeight("auto");

    H1 title = new H1(t("hero.title"));
    Paragraph tagline = new Paragraph(t("hero.tagline"));
    tagline.addClassName("home-tagline");

    Div hero = new Div(logo, title, tagline);
    hero.setId("home-hero");
    hero.addClassName("home-hero");
    return hero;
  }

  private Component createFeaturesSection() {
    Div cards = new Div(
        featureCard(VaadinIcon.USERS, "features.users"),
        featureCard(VaadinIcon.LINK, "features.links"),
        featureCard(VaadinIcon.GROUP, "features.groups"),
        featureCard(VaadinIcon.KEY, "features.roles"),
        featureCard(VaadinIcon.SHIELD, "features.rules"),
        featureCard(VaadinIcon.EYE, "features.views"),
        featureCard(VaadinIcon.USER_CARD, "features.profile"),
        featureCard(VaadinIcon.CONNECT, "features.providers"),
        featureCard(VaadinIcon.MOBILE, "features.responsive"));
    cards.addClassName("home-features");

    return section("home-features", t("features.title"), cards);
  }

  private Card featureCard(VaadinIcon icon, String key) {
    Card card = new Card();
    card.addClassName("home-feature-card");
    Icon prefix = icon.create();
    prefix.addClassName("home-feature-icon");
    card.setHeaderPrefix(prefix);
    card.setTitle(t(key + ".title"));
    card.add(new Paragraph(t(key + ".desc")));
    return card;
  }

  private Component createTryItSection() {
    Paragraph intro = new Paragraph(t("tryit.intro"));

    Div credentials = new Div(
        credentialRow("admin / admin", t("tryit.admin")),
        credentialRow("maria / maria", t("tryit.maria")),
        credentialRow("diego / diego", t("tryit.diego")));
    credentials.addClassName("home-credentials");

    Div actions = new Div(
        actionButton("tryit.users", UsersListView.class, true),
        actionButton("tryit.groups", GroupsListView.class, false),
        actionButton("tryit.roles", AuthoritiesView.class, false),
        actionButton("tryit.rules", RulesView.class, false),
        actionButton("tryit.views", ViewsView.class, false),
        actionButton("tryit.providers", AuthProvidersListView.class, false),
        actionButton("tryit.profile", ProfileView.class, false),
        createTourMenu());
    actions.addClassName("home-actions");

    return section("home-tryit", t("tryit.title"), intro, credentials, actions);
  }

  private Button actionButton(String key, Class<? extends Component> view, boolean primary) {
    Button button = new Button(t(key), e -> getUI().ifPresent(ui -> ui.navigate(view)));
    if (primary) {
      button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    return button;
  }

  private Div credentialRow(String credentials, String description) {
    Span code = new Span(credentials);
    code.addClassName("home-credential-code");
    Div row = new Div(code, new Span(description));
    row.addClassName("home-credential");
    return row;
  }

  private Component createTourMenu() {
    MenuBar menu = new MenuBar();
    menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    SubMenu tours = menu
        .addItem(new Div(VaadinIcon.MAP_MARKER.create(), new Span(t("tour.button")))).getSubMenu();
    // Same labels as the navbar Tour menu: they name the accounts each view accepts, so the
    // visitor picks the right credentials before the tour sends them to a screen they cannot open
    tours.addItem(tourLabel(DemoTour.NAVIGATION),
        e -> DemoTours.start(DemoTour.NAVIGATION, this, this::getTranslation));
    tours.addItem(tourLabel(DemoTour.USERS), e -> startViewTour(DemoTour.USERS,
        UsersListView.class));
    tours.addItem(tourLabel(DemoTour.GROUPS), e -> startViewTour(DemoTour.GROUPS,
        GroupsListView.class));
    tours.addItem(tourLabel(DemoTour.ROLES), e -> startViewTour(DemoTour.ROLES,
        AuthoritiesView.class));
    tours.addItem(tourLabel(DemoTour.RULES), e -> startViewTour(DemoTour.RULES, RulesView.class));
    tours.addItem(tourLabel(DemoTour.VIEWS), e -> startViewTour(DemoTour.VIEWS, ViewsView.class));
    tours.addItem(tourLabel(DemoTour.PROVIDERS), e -> startViewTour(DemoTour.PROVIDERS,
        AuthProvidersListView.class));
    tours.addItem(tourLabel(DemoTour.PROFILE), e -> startViewTour(DemoTour.PROFILE,
        ProfileView.class));
    return menu;
  }

  private String tourLabel(DemoTour tour) {
    return getTranslation(DemoTours.labelKey(tour));
  }

  private void startViewTour(DemoTour tour, Class<? extends Component> view) {
    VaadinSession.getCurrent().setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, tour);
    getUI().ifPresent(ui -> ui.navigate(view));
  }

  private Component createLicenseSection() {
    Paragraph desc = new Paragraph(t("license.desc"));
    Anchor link = new Anchor(APPJARS_SITE_URL, t("license.link"));
    link.setTarget("_blank");
    return section("home-license", t("license.title"), desc, new Paragraph(link));
  }

  private Component createLinksSection() {
    Anchor github = new Anchor(GITHUB_ORG_URL, t("links.github"));
    github.setTarget("_blank");
    Anchor docs = new Anchor(DOCS_URL, t("links.readme"));
    docs.setTarget("_blank");
    Div links = new Div(github, docs);
    links.addClassName("home-links");
    return section("home-links", t("links.title"), links);
  }

  private Div section(String id, String title, Component... content) {
    Div section = new Div();
    section.setId(id);
    section.addClassName("home-section");
    section.add(new H3(title));
    section.add(content);
    return section;
  }

  private String t(String key) {
    return getTranslation(KEY_PREFIX + key);
  }

  @Override
  public String getPageTitle() {
    return t("title");
  }
}
