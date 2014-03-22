# Exposr

Dirt simple build and deployments.

# Usage

Exposr was primarily built to integrate as a very simple, convention-first
build system for Github and plain old git.

It features a rich RESTful API which gives the user complete control while the
system is running.

The most basic configuration takes a github user (or organization), uses the
GitHub API to discover all available repos and registers them with the project.

Any updates will then be built and published by Exposr.

See the bundled [exposr.yaml](/exposr.yaml) file for available configuration
options.

# Building with .exposr.yml

Exposr looks for an .exposr.yml file in the repository.
This file contains instructions for which commands Exposr should run.

The following is a simple example.

```yaml
commands:
  - scripts/build

publish:
  - build
```

This instructs the builder to execute **scripts/build** and publish everything that
exists in the **build** directory.

The published artifacts will have a **project-name** and **id**.
This constitues a deployment and will govern certain aspects of the publishing step.

For example, with the **!local-publisher**.

* The **project-name** will be used as the root directory where everything is copied.
* The **id** will be used to discriminate *unique* deployments, two deployments
  of the same id will overwrite each other.

The exact builder being used is determined by the Exposr configuration and the
server it is running on.

# Components and Configuration

The Exposr service is built up into multiple components.
Each follow a specific structure in the configuration files.
For an example, see the [exposr.yml](/exposr.yml) file.

The root components are.

* **projectManager (require)**
* **repository (required)**
* **publisher (required)**
* **builder (optional, defaults to local-builder)**
* **projectReporter (optional, defaults to memory-project-reporter)**

## Authentication Methods
### Basic Authentication

```yaml
# Authenticate using a username and password combination.
auth:
  !basic-auth
  username: user
  password: password
```

## Project Managers (projectManager)

### Static Project Manager (!static-project-manager)

*project-name* &mdash; *Statically configured*</br>
*id* &mdash; The commit hash of the ref specified.

```yaml
projectManager:
  !static-project-manager
  # Shared authentication method for all specified projects.
  auth: ...
  # List of projects to use.
  projects:
    # Project specific authentication method.
    - auth: ...
      # Project name.
      name: puppet
      # Clone URL.
      url: https://github.com/puppetlabs/puppet
      # Refspec to clone (e.g. HEAD, refs/heads/.., refs/tags/..).
      ref: refs/heads/master
```

### Github Project Manager (!github-project-manager)

*project-name* &mdash; Same as the project name on GitHub.<br />
*id* &mdash; The commit hash of the ref specified.

```yaml
projectManager:
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

## Repository (repository)

Store and retrieve the content of external git repositories.

### Local Repository (!local-repository)

Store everything in the local filesystem.

```yaml
repository:
  !local-repository
  # Path to store all checked out repositories.
  path: /var/exposr/repos
```

## Builder (builder)

How to execute the specified build steps in the **.exposr.yml** manifest.

### Local Builder (!local-builder)

Executes every single command specified in the **commands** section of the maniefst in sequence on the local machine.

*Note: currently has no settings*

```yaml
builder:
  !local-builder {}
```

## Publisher (publisher)

Decides how to send the result of a build to it's destination depending on the **.exposr.yml** manifest.

### Local Publisher (!local-publisher)

Takes the resulting build and publishes everything in the **publish** section to the specified local directory.

This will create a directory structure with the following.

```text
<project-name> -> .builds/<project-name>/<id>
.builds/<project-name>/<id>
```

The **&lt;project-name&gt;** symlink is created atomically using an overwriting rename.

```yaml
publisher:
  !local-publisher
  # Path to publish projects to.
  path: /var/exposr/publish
```

## Project Reporter (projectReporter)

Is responsible for reporting the state of all discovered projects so they can be made available through the REST API.

### Memory Project Reporter (!memory-project-reporter)

Retains the current state in memory, is lost on restarts.

```yaml
projectReporter:
  !memory-project-reporter {}
```
