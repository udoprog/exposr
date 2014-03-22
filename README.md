# Exposr

Dirt simple build and deployment server.

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

## How projects are built (.exposr.yml)

Exposr looks for an .exposr.yml file in the repository.
This file contains instructions for which commands Exposr should run.

The following is a very simple example.

```yaml
commands:
  - scripts/build

publish:
  - build
```

This instructs the builder to execute scripts/build and publish everything that
exists in the build directory.

The exact builder being used is determined by the Exposr configuration and the
server it is running on.

Check [exposr.yml]
