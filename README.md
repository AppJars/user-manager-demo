# User Manager demo

A small Spring Boot + Vaadin application that runs the [User Manager
AppJar](https://docs.appjars.com/user-manager/overview/) as it ships. It is meant for evaluating the
appjar: users, groups, roles, external authentication providers and runtime access rules, with
ready-made screens, services and database schema, so the application itself contains nothing but
its landing page, its layout and its sample data.

Start it, open the landing page and take a guided tour of any screen — no configuration needed.

## What comes preloaded

The first start seeds a small dataset and keeps it in a local H2 database under `data/`:

| | |
| --- | --- |
| Accounts | `admin`, `maria`, `diego` (password same as the username) |
| Roles | `ADMIN`, `SUPPORT`, `VIEWER` |
| Groups | `Support team`, carrying `SUPPORT` |
| Access rules | Four: roles/rules/views for administrators, users/groups for administrators and the support team, a pattern rule covering the auth provider screens, and a disabled one you can switch on |

The three accounts deliberately reach different screens, so the access rules can be seen deciding.
Delete the `data/` folder to go back to this state.

## Prerequisites

- JDK 21 or newer
- Maven 3.9 or newer (or the Maven wrapper of your IDE)

## Running it

1. `mvn` — the default goal is `spring-boot:run`. The first build downloads the Vaadin frontend, so
   it takes a few minutes.
2. Wait for `Started Application in N seconds`.
3. Open <http://localhost:8080> (the browser is launched for you).
4. Sign in with any of the accounts above.

Stop it with `Ctrl+C`.

## Using the demo

The landing page is public and lists the features, the demo accounts and the guided tours. Every
other screen requires signing in, and which ones you reach depends on the account:

| Screen | Route | What you can do | Reachable by |
| --- | --- | --- | --- |
| Users | `um/users` | Create, edit, enable, disable and delete accounts; hand out registration and password reset links; filter by name, availability and link status | `admin`, `maria` |
| Groups | `um/groups` | Bundle roles into groups and grant them to many users at once | `admin`, `maria` |
| Roles | `um/roles` | Create and rename roles, and see how many users and groups hold each one | `admin` |
| Auth providers | `um/auth-providers` | Register the OAuth2 providers users may sign in through | `admin` |
| Access rules | `um/access-rules` | Add, reorder, disable and delete the rules that decide who reaches which URL, and preview the access of any role | `admin` |
| Views | `um/views` | Audit every registered route: the rule that governs it and the roles it lets through | `admin` |
| My profile | `um/profile` | See your groups and roles, and change your password | any account |

Guided tours walk each of those screens. Start one from **Guided tour** on the landing page, or
from the **Tour** menu in the header while you are inside any view; *This page* starts the tour of
the screen you are on.

Enabling an external authentication provider additionally needs
`spring-boot-starter-oauth2-client` on the classpath, which this demo leaves out.

## Configuration

Everything below lives in `src/main/resources/application.properties`. The demo sets the appjar
defaults explicitly, so the file doubles as the list of what can be configured.

### View routes

| Property | Default | Screen |
| --- | --- | --- |
| `com.appjars.usermanager.url.users` | `um/users` | User list |
| `com.appjars.usermanager.url.users-create` | `um/users/create` | New user |
| `com.appjars.usermanager.url.users-edit` | `um/users/edit` | Edit user |
| `com.appjars.usermanager.url.user-registration-link` | `um/users/register` | Public registration link |
| `com.appjars.usermanager.url.password-change-link` | `um/users/password-reset` | Public password reset link |
| `com.appjars.usermanager.url.groups` | `um/groups` | Group list |
| `com.appjars.usermanager.url.groups-create` | `um/groups/create` | New group |
| `com.appjars.usermanager.url.groups-edit` | `um/groups/edit` | Edit group |
| `com.appjars.usermanager.url.roles` | `um/roles` | Role list |
| `com.appjars.usermanager.url.profile` | `um/profile` | Profile of the signed-in user |
| `com.appjars.usermanager.url.access-rules` | `um/access-rules` | Access rules |
| `com.appjars.usermanager.url.views` | `um/views` | View audit |
| `com.appjars.usermanager.url.auth-providers` | `um/auth-providers` | External authentication providers |

### Registration and password reset links

| Property | Default | What it does |
| --- | --- | --- |
| `com.appjars.usermanager.default.days.expiration` | `7` | Days a new link stays valid |
| `com.appjars.usermanager.maximum.days.expiration` | `99` | Highest number of days the dialog accepts |
| `com.appjars.usermanager.external-url` | `http://localhost:8080/` | Base URL the generated links point at |
| `com.appjars.usermanager.links.redirect.url` | `/` | Where a user is sent once the link has been used |

### Security

| Property | Default | What it does |
| --- | --- | --- |
| `com.appjars.usermanager.encoding.secret.key` | `1234567890` | Key used to encode passwords |
| `com.appjars.usermanager.external-auth.secret.key` | *(none)* | Base64 AES-256 key that encrypts stored OAuth2 client secrets |
| `com.appjars.usermanager.security.permit-public-links` | `true` | Whether the appjar declares the registration, password reset and external auth paths as public |

## License mode

The demo runs unlicensed, which caps it at **5 users in total**. Every other feature is fully
functional. A full license removes the limit and changes nothing else — see
[appjars.com](https://www.appjars.com).
