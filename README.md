# Exposr

Dirt simple building and deployments using *strong convention* over configuration.

# Usage

Exposr was primarily built to integrate as a very simple build system for github.

It features a rich RESTful API which gives the user complete control while the
system is running.

The most basic configuration takes a github user (or organization), uses the
GitHub API to discover all available repos and registers them with the project.

Any updates will then be built and published by Exposr.

See the bundled [exposr.yaml](/exposr.yaml) file for an example configuration, or the [Service Configuration](#Server Configuration) section for more detailed documentation.

# Building

Exposr looks for a file named **.exposr.yml** in the root of your repository.

This file contains instructions for which commands Exposr should run and what should be published after these has been executed.

The following is an example **.exposr.yml** file.

```yaml
commands:
  - scripts/build

publish:
  - build
```

The above instructs the builder to execute **scripts/build** and publish everything that
exists in the **build** directory.

The published artifacts will have a *project-name* and an *id* associated with them.
The exact detail of what this means differs between project managers.
Each *project-name* can only have one *id* deployed at a time.
This combination constitutes a *deployment*.

###### Example when using the [static-project-manager](#static-project-manager) and the [local-publisher](#local-publisher) components

The **project-name** will be used as the root directory where everything is published and is statically configured in the **projectManager** section of the service configuration.

The **id** will be used to discriminate *unique* deployments and will be the current commit id of the ref configuration in the **projectManager** section.

Two deployments of the same id will not co-exist and will overwrite each other depending on which order they were published.

# Service Configuration

The Exposr service is built up into multiple components.
Each follow a specific structure in the configuration file.

For a complete example, see the [exposr.yaml](/exposr.yaml) example configuration.

The available root configuration keys are.

* [**projectManager**](#Project Manager) &mdash; **required**
  * [static-project-manager](#static-project-manager)
  * [github-project-manager](#github-project-manager)
* [**repository**](#Repository) &mdash; **required**
  * [local-repository](#local-repository)
* [**publisher**](#Publisher) &mdash; **required**
  * [local-publisher](#local-publisher)
  * [remote-publisher](#remote-publisher)
* [**builder**](#Builder) &mdash; **optional**, defaults to **local-builder**.
  * [local-builder](#local-builder)
* [**projectReporter**](#Project Reporter) &mdash; **optional**, defaults to **memory-project-reporter**.
  * [memory-project-reporter](#memory-project-reporter)

#### Project Manager
Key: **projectManager**

Is responsible for discovering new projects.

Typically contacts external services in order to retrieve a list of projects matching the configured criteria.

The simplest project manager **static-project-manager** takes a static list of configuration.

##### static-project-manager:

**project-name** &mdash; *Statically configured*<br />
**id** &mdash; The commit hash of the ref specified.

```yaml
!static-project-manager
# Shared authentication method for all specified projects.
auth: ...
# List of projects to use.
projects:
  - # Project specific authentication method.
    # See the Authentication section for more details.
    auth: ...
    # Project name.
    name: puppet
    # Remote URL.
    url: https://github.com/puppetlabs/puppet
    # Refspec to clone (e.g. HEAD, refs/heads/.., refs/tags/..).
    ref: refs/heads/master
```

##### github-project-manager:

**project-name** &mdash; Same as the project name on GitHub.<br />
**id** &mdash; The commit hash of the ref specified.

```yaml
!github-project-manager
# API endpoint to use, useful when you are running GHE.
apiUrl: https://api.github.com
 # User or organization to discover projects on.
user: udoprog
# The authentication method to use.
auth:
  !basic-auth
  username: user
  password: password
```

#### Repository

Key: **repository**

Store and retrieve the content of external git repositories.

##### local-repository:
Store everything in the local filesystem.

```yaml
!local-repository
# Path to store all checked out repositories.
path: /var/exposr/repos
```

#### Builder

Key: **builder**

How to execute the specified build steps in the **.exposr.yml** manifest.

##### local-builder:

Executes every single command specified in the **commands** section of the maniefst in sequence on the local machine.

```yaml
# Local builder has no configuration options.
# Configure using the empty map '{}'.
!local-builder {}
```

#### Publisher

Key: **publisher**

Decides how to send the result of a build to it's destination depending on the **.exposr.yml** manifest.

##### local-publisher:

Takes the resulting build and publishes everything in the **publish** section to the specified local directory.

This will setup the following directory structure.

```text
<project-name> -> .builds/<project-name>/<id>
.builds/<project-name>/<id-1>
.builds/<project-name>/<id-2>
...
```

**&lt;project-name&gt;** is a symlink which is atomically updated using
a rename.

```yaml
publisher:
  !local-publisher
  # Path to publish projects to.
  path: /var/exposr/publish
```

#### Project Reporter

Key: **projectReporter**

Is responsible for reporting the state of all discovered projects so they can be made available through the REST API.

##### memory-project-reporter:

Retains the current state in memory, is lost on restarts.

```yaml
!memory-project-reporter {}
```

#### Authentication Methods

Certain components require or support authentication methods.

When these are required, the following structure is supported.

*Note:* Authentication methods are typically designated with the **auth:** configuration key, but this might vary.

##### basic-auth

```yaml
# Authenticate using a username and password combination.
!basic-auth
username: user
password: password
```

# REST API

#### Document Types

##### ErrorMessage

An object describing that an error has occured.

```json
{"message": <string>}
```

The **message** attribute describes the error that occured, this message is suitable to show to the end user.

More information can typically be derived by inspecting the HTTP status code directly.

##### TaskResponse

An object describing pending or newly created tasks.

```json
{"link": <url>, "output": <url>}
```

The **link** attribute describes the URL to get more information about the newly created tas.

The **output** attribute describes the URL to get Server-Sent Events for output from a specific task.

#### Error Handling

All resources can safely be assumed to have the content type **application/json** and be valid JSON documents.

All errors are indicated with an appropriate status code.

Any non-OK response should be treated as an **ErrorMessage** document.

A good check to see if an error occured would be to inspect the *family* of the status code.

**2xx** &mdash; OK.<br />
**3xx** &mdash; Redirects which should be followed until a more appropriate status code is encountered.<br />
**4xx** &mdash; Client error, see the **ErrorMessage** object.<br />
**5xx** &mdash; Server error, the structure of the message is unrealiable since it might have originated before serializers and handlers have been setup on the service. Check the *Content-Type* of the result and assert that it is **application/json** before attempting to parse an **ErrorMessage** object.

#### Endpoints

All endpoints are under the special **/_exposr** resource and should always be prefixed with this.

For example, the real request to get brief information about all projects would be **GET /_exposr/project**.

###### POST /build
Build all projects.

Returns a list of **TaskResponse** objects.

###### POST /sync
Synchronize all projects.

Returns a list of **TaskResponse** objects.

###### GET /project
Show *brief* information about all projects.

###### GET /project/<name>
Show *detailed* information about a single project.

###### POST /project/<name>/build
Trigger a build of a single project.

Will return a **TaskResponse** object.

###### POST /project/<name>/sync
Trigger a sync of a single project.

Will return a **TaskResponse** object.

###### GET /task
Get *brief* information about all tasks.

###### GET /task/<id>
Get *detailed* information about a single task.

###### GET /tasks/<id>/output
Setup a Server-Sent Event stream to the output of a specific task.
