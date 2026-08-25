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

package com.appjars.usermanager.demo.views.tour;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.vaadin.addons.antlerflow.tour.EngineType;
import org.vaadin.addons.antlerflow.tour.Tour;
import org.vaadin.addons.antlerflow.tour.TourButton;
import org.vaadin.addons.antlerflow.tour.TourButtonType;
import org.vaadin.addons.antlerflow.tour.TourStep;

/**
 * Factory of the guided tours offered by the demo. Every step is anchored to an element id exposed
 * by the User Manager views - never to a structural selector, which would break the moment a view
 * is rearranged.
 *
 * <p>Steps never point at the raw selector directly: a small client-side resolver tags the first
 * <em>visible</em> match of each selector with a {@code data-antler-target} marker and the step
 * attaches to that marker. A selector that matches nothing gets no marker, and the step is
 * rendered centered, which is also how the intro and summary steps are declared.
 */
public final class DemoTours {

  /** Session attribute used to start a tour after navigating (and logging in) to its view. */
  public static final String PENDING_TOUR_ATTRIBUTE = DemoTours.class.getName() + ".pendingTour";

  static final String KEY_PREFIX = "appjars.usermanager.demo.tour.";

  public enum DemoTour {
    HOME, NAVIGATION, PROFILE, USERS, GROUPS, ROLES, RULES, VIEWS, PROVIDERS
  }

  /**
   * A step definition. {@code selector} is the real element selector; the {@code attachTo} handed
   * to the engine is the marker the resolver places on it. A {@code null} selector means the step
   * is rendered centered. {@code action}, if set, holds the step back until the next one has a
   * target.
   */
  private record StepDef(String key, String selector, String position, StepAction action,
      boolean first, boolean last) {}

  /**
   * What the tour clicks when the visitor leaves a step, so the next step has something to point
   * at. Opening an editor goes through Flow and the engine resolves a step's element the moment it
   * shows it, so the click happens on Next and the engine is held back until {@code awaitVisible}
   * is laid out.
   */
  private record StepAction(String click, String awaitVisible) {}

  /**
   * The editor a tour walks into, and the control that gets back out of it. {@code present} is a
   * piece of the editor's own content rather than its container: a Flow {@code Dialog} host is
   * permanently {@code display:none} and the visible overlay is a separate element, so content is
   * what tells the two states apart. While the tour is inside, {@code present} disappearing means
   * the visitor left by hand and the tour ends with them - the remaining steps point in there. When
   * the tour ends on its own, {@code leave} is clicked, so the visitor is put back where the tour
   * found them and nothing is saved. Tours that never leave their view have no detour.
   */
  private record Detour(String present, String leave) {}

  /** Marker attribute written by {@link #RESOLVE_TARGETS_JS} on the first visible match. */
  private static final String TARGET_ATTR = "data-antler-target";

  /**
   * The row of matching inputs, which stands for "the rule editor is open": it is always visible
   * within the editor, and nowhere else.
   */
  private static final String RULE_EDITOR = "#rule-inputs-layout";

  /** The account editor, which the users tour reaches by route rather than in an overlay. */
  private static final String USER_EDITOR = "#user-view";

  /**
   * $0 is a JSON map of {stepId: cssSelector}. Tags the first visible match of each selector.
   *
   * <p>The marker holds a whitespace-separated list of step ids rather than a single one, and steps
   * match it with {@code ~=}. Several steps legitimately point at the same element - the rules tour
   * describes the grid and then its priority column, both on the rules grid - and with one id per
   * marker the later step overwrote the earlier one's tag, leaving that step to render centered as
   * if its target did not exist.
   */
  private static final String RESOLVE_TARGETS_JS = """
      const MAP = JSON.parse($0);
      const ATTR = 'data-antler-target';
      const ids = el => (el.getAttribute(ATTR) || '').split(/\\s+/).filter(Boolean);
      const write = (el, list) => {
        if (list.length) { el.setAttribute(ATTR, list.join(' ')); } else { el.removeAttribute(ATTR); }
      };
      const resolve = () => {
        Object.keys(MAP).forEach(id => {
          let pick = null;
          for (const el of document.querySelectorAll(MAP[id])) {
            const r = el.getBoundingClientRect();
            if (r.width > 4 && r.height > 4) { pick = el; break; }
          }
          document.querySelectorAll('[' + ATTR + '~="' + id + '"]')
              .forEach(el => { if (el !== pick) { write(el, ids(el).filter(t => t !== id)); } });
          if (pick && !ids(pick).includes(id)) { write(pick, ids(pick).concat(id)); }
        });
      };
      if (window.__antlerResolver) { window.__antlerResolver.stop(); }
      let scheduled = false;
      const schedule = () => { if (scheduled) return; scheduled = true;
        requestAnimationFrame(() => { scheduled = false; resolve(); }); };
      resolve();
      const obs = new MutationObserver(schedule);
      obs.observe(document.body, {childList: true, subtree: true, attributes: true,
          attributeFilter: ['hidden', 'style', 'class']});
      window.__antlerResolver = { resolve, stop() { obs.disconnect();
        document.querySelectorAll('[' + ATTR + ']').forEach(el => el.removeAttribute(ATTR));
        window.__antlerResolver = null; } };
      """;

  /**
   * Three fixes, all inert once no step is active:
   *
   * <ul>
   * <li>Driver forces {@code overflow:hidden} on the parent of the highlighted element, which clips
   * the siblings sharing its row (the filter fields next to a highlighted toolbar button). Inside a
   * dialog the clipping ancestor is not the direct parent but the form wrapping the field, hence
   * the second, descendant-matching rule.
   * <li>A modal dialog turns off pointer events on the rest of the document, which would reach the
   * popover through inheritance and leave its buttons dead.
   * <li>Driver's dim-and-cut-out highlight paints below a top-layer overlay, so inside one - the
   * rule editor, or the submenu of a row's actions menu - the active element gets an outline of its
   * own instead.
   * </ul>
   */
  private static final String TOUR_CSS_JS = """
      if (!document.getElementById('demo-tour-css')) {
        const style = document.createElement('style');
        style.id = 'demo-tour-css';
        style.textContent = [
            'body :not(body):has(> .driver-active-element) { overflow: visible !important; }',
            'vaadin-dialog-overlay :not(body):has(.driver-active-element)',
            '    { overflow: visible !important; }',
            '.driver-popover { pointer-events: auto !important; }',
            'vaadin-dialog-overlay .driver-active-element,',
            '[popover] .driver-active-element {',
            '  outline: 2px solid var(--lumo-primary-color);',
            '  outline-offset: 4px;',
            '  border-radius: var(--lumo-border-radius-m);',
            '}'].join('\\n');
        document.head.appendChild(style);
      }
      """;

  /**
   * Keeps the popover on top of the overlays a tour steps into. Vaadin 24.5+ renders its overlays in
   * the browser top layer through the native Popover API, where z-index cannot compete: the popover
   * has to join the top layer too, or it paints behind them. Paint order there follows the last
   * {@code showPopover()} call, so it is re-asserted whenever another overlay opens - and the
   * promotion is undone as soon as the last of them is gone, because it makes the viewport the
   * popover's containing block and that misplaces steps anchored to ordinary page elements.
   *
   * <p>$0 is a JSON array of the overlay selectors the tour walks into; only the steps anchored
   * inside one of them are ever shown while the promotion is on.
   */
  private static final String TOP_LAYER_JS = """
      const OVERLAYS = JSON.parse($0);
      if (window.__antlerTopLayer) { window.__antlerTopLayer.stop(); }
      const promote = () => document.querySelectorAll('.driver-popover').forEach(el => {
        if (el.getAttribute('popover') !== 'manual') { el.setAttribute('popover', 'manual'); }
        el.style.margin = '0';
        try { if (!el.matches(':popover-open')) { el.showPopover(); } } catch (e) { /* n/a */ }
      });
      const demote = () => document.querySelectorAll('.driver-popover[popover]').forEach(el => {
        try { el.hidePopover(); } catch (e) { /* n/a */ }
        el.removeAttribute('popover');
        el.style.margin = '';
      });
      let promoted = false;
      const onToggle = (e) => {
        const el = document.querySelector('.driver-popover');
        if (e.newState === 'open' && el && el !== e.target && promoted
            && el.matches(':popover-open')) {
          try { el.hidePopover(); el.showPopover(); } catch (err) { /* n/a */ }
        }
      };
      const sync = () => {
        const open = OVERLAYS.some(sel => {
          const el = document.querySelector(sel);
          return !!el && el.getBoundingClientRect().height > 4;
        });
        if (open) {
          promote();
          promoted = true;
        } else if (promoted) {
          demote();
          promoted = false;
        }
      };
      document.addEventListener('toggle', onToggle, true);
      const overlayObs = new MutationObserver(sync);
      overlayObs.observe(document.body, {childList: true, subtree: true, attributes: true,
          attributeFilter: ['opened', 'hidden', 'style', 'popover']});
      sync();
      window.__antlerTopLayer = { stop() { overlayObs.disconnect();
        document.removeEventListener('toggle', onToggle, true);
        demote();
        window.__antlerTopLayer = null; } };
      """;

  /**
   * Ends the tour when the visitor leaves the editor it walked into by hand, because every step left
   * points inside it. Only a disappearance counts, so nothing happens until the editor has actually
   * been seen: the hook is installed while the tour is still on its own view. $0 is the selector
   * that stands for the editor being there.
   */
  private static final String ABANDON_JS = """
      const EDITOR = $0;
      if (window.__antlerAbandon) { window.__antlerAbandon.stop(); }
      let wasOpen = false;
      const check = () => {
        const el = document.querySelector(EDITOR);
        const open = !!el && el.getBoundingClientRect().height > 4;
        if (wasOpen && !open) { window.AntlerTour?.cancel(); }
        wasOpen = open;
      };
      const editorObs = new MutationObserver(check);
      editorObs.observe(document.body, {childList: true, subtree: true});
      check();
      window.__antlerAbandon = { stop() { editorObs.disconnect();
        window.__antlerAbandon = null; } };
      """;

  /**
   * Lets the tour operate the application, and holds a step back until the next one has something
   * to point at. $0 is a JSON map of {stepId: action}; the step being left is read from the marker
   * the resolver put on the highlighted element, so only anchored steps can carry an action - which
   * is all they need to, since an action exists to reveal the <em>next</em> step's target.
   *
   * <p>The click on the primary button is caught in the capture phase, before the engine's own
   * handler on the button itself, and swallowed. Once the awaited element is laid out the click is
   * replayed and the engine advances as usual; if it never appears the wait gives up after three
   * seconds and the next step falls back to being centered, same as any unresolved target. Back
   * buttons carry {@code secondary} and are never intercepted; replaying an action after Back is
   * harmless, both of them are idempotent.
   */
  private static final String STEP_ACTIONS_JS = """
      const ACTIONS = JSON.parse($0);
      const ATTR = 'data-antler-target';
      const TIMEOUT = 3000;
      if (window.__antlerActions) { window.__antlerActions.stop(); }
      const laidOut = el => { const r = el.getBoundingClientRect();
        return r.width > 4 && r.height > 4; };
      let replaying = false;
      const onClick = (e) => {
        if (replaying || !e.target.closest) { return; }
        const btn = e.target.closest('.driver-button');
        if (!btn || btn.classList.contains('secondary')) { return; }
        const active = document.querySelector('.driver-active-element');
        // The marker carries every step id resolved to this element, so look each one up.
        const stepIds = active ? (active.getAttribute(ATTR) || '').split(/\\s+/).filter(Boolean) : [];
        const action = stepIds.map(id => ACTIONS[id]).find(Boolean);
        if (!action) { return; }
        e.preventDefault();
        e.stopPropagation();
        document.querySelector(action.click)?.click();
        const deadline = performance.now() + TIMEOUT;
        const tick = () => {
          const el = document.querySelector(action.await);
          if ((el && laidOut(el)) || performance.now() > deadline) {
            // Tag the editor's targets before handing control back: the engine resolves the next
            // step's element the instant the button is clicked, and the resolver's own pass is only
            // scheduled for the next frame, so without this the first step inside the editor raced
            // it and rendered centered.
            window.__antlerResolver?.resolve?.();
            replaying = true;
            btn.click();
            replaying = false;
          } else {
            requestAnimationFrame(tick);
          }
        };
        tick();
      };
      document.addEventListener('click', onClick, true);
      window.__antlerActions = { stop() {
        document.removeEventListener('click', onClick, true);
        window.__antlerActions = null; } };
      """;

  /** Tears down every helper installed for the tour. */
  private static final String STOP_JS = """
      if (window.__antlerResolver) { window.__antlerResolver.stop(); }
      if (window.__antlerActions) { window.__antlerActions.stop(); }
      if (window.__antlerTopLayer) { window.__antlerTopLayer.stop(); }
      if (window.__antlerAbandon) { window.__antlerAbandon.stop(); }
      document.getElementById('demo-tour-css')?.remove();
      """;

  /** Leaves no editor behind when the tour ends. $0 is the editor, $1 the control that leaves it. */
  private static final String LEAVE_JS =
      "if (document.querySelector($0)) { document.querySelector($1)?.click(); }";

  private DemoTours() {}

  /**
   * i18n key of the menu label of a tour, shared by the landing page and the navbar Tour menu. The
   * label also names the demo accounts the tour's view accepts.
   */
  public static String labelKey(DemoTour tour) {
    return KEY_PREFIX + tour.name().toLowerCase();
  }

  public static Tour create(DemoTour tour, SerializableFunction<String, String> translator) {
    List<TourStep> steps = steps(tour).stream().map(def -> step(def, translator)).toList();
    return Tour.builder().engineType(EngineType.DRIVER).steps(steps).showCancelButton(true)
        .allowClose(true).build();
  }

  /**
   * Creates the tour, attaches it to {@code host} and starts it, detaching it again once it is
   * completed or canceled.
   */
  public static void start(DemoTour tour, Component host,
      SerializableFunction<String, String> translator) {
    Tour t = create(tour, translator);
    List<StepDef> defs = steps(tour);
    Detour detour = detour(tour);
    List<String> overlays = topLayerOverlays(tour);
    host.getElement().appendChild(t.getElement());
    host.getElement().executeJs(TOUR_CSS_JS);
    host.getElement().executeJs(RESOLVE_TARGETS_JS, targetJson(defs));
    host.getElement().executeJs(STEP_ACTIONS_JS, actionJson(defs));
    if (!overlays.isEmpty()) {
      host.getElement().executeJs(TOP_LAYER_JS, selectorJson(overlays));
    }
    if (detour != null) {
      host.getElement().executeJs(ABANDON_JS, detour.present());
    }
    t.addTourCompletedListener(e -> stop(t, host, detour));
    t.addTourCanceledListener(e -> stop(t, host, detour));
    t.start();
  }

  private static void stop(Tour tour, Component host, Detour detour) {
    // Order matters: the hooks go first, so leaving the editor below is not mistaken for the
    // visitor abandoning the tour.
    host.getElement().executeJs(STOP_JS);
    if (detour != null) {
      host.getElement().executeJs(LEAVE_JS, detour.present(), detour.leave());
    }
    tour.getElement().removeFromParent();
  }

  /**
   * The two tours that walk into an editor: the rule editor, which is a modal, and the account
   * editor, which is a route of its own. Both are left through their Cancel button.
   */
  private static Detour detour(DemoTour tour) {
    return switch (tour) {
      case RULES -> new Detour(RULE_EDITOR, "#cancel-button");
      case USERS -> new Detour(USER_EDITOR, "#cancel-button");
      default -> null;
    };
  }

  /**
   * The overlays a tour anchors a step inside. Only the rules tour does, in the rule editor. The
   * account editor is an ordinary view, so its steps need no promotion - and would be misplaced by
   * one.
   */
  private static List<String> topLayerOverlays(DemoTour tour) {
    return tour == DemoTour.RULES ? List.of(RULE_EDITOR) : List.of();
  }

  private static List<StepDef> steps(DemoTour tour) {
    return switch (tour) {
      case HOME -> homeSteps();
      case NAVIGATION -> navigationSteps();
      case PROFILE -> profileSteps();
      case USERS -> usersSteps();
      case GROUPS -> groupsSteps();
      case ROLES -> rolesSteps();
      case RULES -> rulesSteps();
      case VIEWS -> viewsSteps();
      case PROVIDERS -> providersSteps();
    };
  }

  /**
   * Walks the landing page section by section. Every step anchors to a section id the page already
   * exposes, and none of them depends on being signed in, so this is the one tour an anonymous
   * visitor can take from end to end.
   */
  private static List<StepDef> homeSteps() {
    return List.of(
        intro("home.intro"),
        def("home.features", "#home-features", "top", false, false),
        def("home.tryit", "#home-tryit", "top", false, false),
        def("home.license", "#home-license", "top", false, false),
        def("home.links", "#home-links", "top", false, true));
  }

  /**
   * Walks the drawer, one step per view, and closes by inviting the visitor to compare the demo
   * accounts. Anonymously the drawer only offers Home, so the steps for the views the visitor
   * cannot reach yet resolve to nothing and are rendered centered - which is exactly the point the
   * closing step makes.
   */
  private static List<StepDef> navigationSteps() {
    return List.of(
        intro("navigation.intro"),
        def("navigation.home", "#nav-home", "right", false, false),
        def("navigation.profile", "#nav-profile", "right", false, false),
        def("navigation.users", "#nav-users", "right", false, false),
        def("navigation.groups", "#nav-groups", "right", false, false),
        def("navigation.roles", "#nav-roles", "right", false, false),
        def("navigation.providers", "#nav-providers", "right", false, false),
        def("navigation.rules", "#nav-rules", "right", false, false),
        def("navigation.views", "#nav-views", "right", false, false),
        def("navigation.accounts", null, null, false, true));
  }

  private static List<StepDef> profileSteps() {
    return List.of(
        intro("profile.intro"),
        def("profile.identity", "#user-avatar", "bottom", false, false),
        def("profile.groups", "#groups-badge-list", "bottom", false, false),
        def("profile.roles", "#authorities-badge-list", "top", false, false),
        def("profile.password", "#change-password-button", "top", false, false),
        def("profile.finish", null, null, false, true));
  }

  /**
   * Walks the list and then the account editor. New User is clicked for the tour - there is nothing
   * to learn from pressing it - and the wait absorbs the navigation; the editor's own steps anchor to
   * plain view elements, so from there on nothing special is needed. The tour cancels out of the
   * editor when it ends, which is also why it never asks the visitor to fill anything in.
   */
  private static List<StepDef> usersSteps() {
    return List.of(
        intro("users.intro"),
        def("users.grid", "#users-grid", "top", false, false),
        def("users.filters", "#filter-button", "bottom", false, false),
        def("users.actions", "#users-grid", "top", false, false),
        acting("users.create", "#new-user-button", "bottom",
            click("#new-user-button", USER_EDITOR)),
        def("users.username", "#username-textfield", "bottom", false, false),
        def("users.roles", "#authorities-combo-box", "bottom", false, false),
        def("users.summary", "#summary-section", "top", false, false),
        def("users.limit", null, null, false, true));
  }

  private static List<StepDef> groupsSteps() {
    return List.of(
        intro("groups.intro"),
        def("groups.grid", "#groups-grid", "top", false, false),
        def("groups.filters", "#groups-filter-button", "bottom", false, false),
        def("groups.create", "#new-group-button", "bottom", false, false),
        def("groups.finish", null, null, false, true));
  }

  private static List<StepDef> rolesSteps() {
    return List.of(
        intro("roles.intro"),
        def("roles.grid", "#authorities-grid", "top", false, false),
        def("roles.filters", "#filter-button", "bottom", false, false),
        def("roles.create", "#new-role-button", "bottom", false, false),
        def("roles.finish", null, null, false, true));
  }

  /**
   * The only tour that steps inside an overlay. It opens the rule editor itself - there is nothing
   * to learn from pressing New Rule - and then stays put: the advanced panel is described rather
   * than expanded, so the tour never leaves the visitor a form in a state they did not choose, and
   * every remaining step points at something the editor shows from the moment it opens.
   *
   * <p>That last condition is what decides which fields get a step of their own. A new rule starts
   * on the {@code ALL_OF} criteria, which is role based, so both the authorization combo and the
   * roles field it governs are laid out as soon as the editor opens. The query-parameters checkbox
   * is not: it only shows for the pattern types that match a full URL, and a new rule starts as
   * SIMPLE. It is described from the advanced step rather than anchored, so the tour does not have
   * to change the rule type to point at it.
   */
  private static List<StepDef> rulesSteps() {
    return List.of(
        intro("rules.intro"),
        def("rules.grid", "#rules-grid", "top", false, false),
        def("rules.priority", "#rules-grid", "top", false, false),
        def("rules.filters", "#filter-button", "bottom", false, false),
        def("rules.roleaccess", "#role-access-button", "bottom", false, false),
        acting("rules.create", "#new-rule-button", "bottom",
            click("#new-rule-button", RULE_EDITOR)),
        def("rules.views", "#views-combobox", "bottom", false, false),
        def("rules.advanced", "#advanced-options-accordion-panel", "bottom", false, false),
        def("rules.authorization", "#authorization-type-combobox", "top", false, false),
        def("rules.roles", "#roles-combobox", "top", false, false),
        def("rules.finish", null, null, false, true));
  }

  private static List<StepDef> viewsSteps() {
    return List.of(
        intro("views.intro"),
        def("views.grid", "#views-grid", "top", false, false),
        def("views.details", "#views-grid", "top", false, false),
        def("views.filters", "#views-filter-button", "bottom", false, false),
        def("views.finish", null, null, false, true));
  }

  /** Walks the external authentication providers, which the demo ships without any of. */
  private static List<StepDef> providersSteps() {
    return List.of(
        intro("providers.intro"),
        def("providers.list", "#auth-providers-list-view vaadin-grid", "top", false, false),
        def("providers.create", "#new-auth-provider-button", "bottom", false, false),
        def("providers.finish", null, null, false, true));
  }

  /** Opening step of a tour: centered, so the view is laid out before any anchored step shows. */
  private static StepDef intro(String key) {
    return new StepDef(key, null, null, null, true, false);
  }

  private static StepDef def(String key, String selector, String position, boolean first,
      boolean last) {
    return new StepDef(key, selector, position, null, first, last);
  }

  /** A step that prepares the next one when the visitor moves on. */
  private static StepDef acting(String key, String selector, String position, StepAction action) {
    return new StepDef(key, selector, position, action, false, false);
  }

  private static StepAction click(String selector, String awaitVisible) {
    return new StepAction(selector, awaitVisible);
  }

  private static TourStep step(StepDef def, SerializableFunction<String, String> t) {
    List<TourButton> buttons = new ArrayList<>();
    if (!def.first()) {
      buttons.add(TourButton.builder().label(t.apply(KEY_PREFIX + "btn.back")).secondary(true)
          .type(TourButtonType.PREVIOUS).build());
    }
    buttons.add(
        TourButton.builder().label(t.apply(KEY_PREFIX + (def.last() ? "btn.done" : "btn.next")))
            .type(TourButtonType.NEXT).build());
    String stepId = stepId(def);
    return TourStep.builder().id(stepId)
        .attachTo(def.selector() == null ? null : "[" + TARGET_ATTR + "~='" + stepId + "']")
        .position(def.position()).title(t.apply(KEY_PREFIX + def.key() + ".title"))
        .content(t.apply(KEY_PREFIX + def.key() + ".desc")).buttons(buttons).build();
  }

  private static String stepId(StepDef def) {
    return def.key().replace('.', '-');
  }

  /** Builds the {stepId: selector} map consumed by {@link #RESOLVE_TARGETS_JS}. */
  private static String targetJson(List<StepDef> defs) {
    return defs.stream().filter(def -> def.selector() != null)
        .map(def -> "\"" + stepId(def) + "\":\"" + def.selector() + "\"")
        .collect(Collectors.joining(",", "{", "}"));
  }

  /** Builds the selector array consumed by {@link #TOP_LAYER_JS}. */
  private static String selectorJson(List<String> selectors) {
    return selectors.stream().map(sel -> "\"" + sel + "\"")
        .collect(Collectors.joining(",", "[", "]"));
  }

  /** Builds the {stepId: action} map consumed by {@link #STEP_ACTIONS_JS}. */
  private static String actionJson(List<StepDef> defs) {
    return defs.stream().filter(def -> def.action() != null)
        .map(def -> "\"" + stepId(def) + "\":" + actionJson(def.action()))
        .collect(Collectors.joining(",", "{", "}"));
  }

  private static String actionJson(StepAction action) {
    return "{\"click\":\"" + action.click() + "\",\"await\":\"" + action.awaitVisible() + "\"}";
  }
}
