# victims-plugin-jenkins [![Build Status](https://travis-ci.org/isaacanderson/victims-plugin-jenkins.png)](https://travis-ci.org/isaacanderson/victims-plugin-jenkins)

## About

This Jenkins-CI plugin provides the functionality to scan a Java projects dependencies against a database of publicly 
known vulnerabilities. The canonical version of the database is hosted at https://victi.ms and is maintained by
Red Hat security teams.

## Getting Started

To build the plugin simply run 
```sh
  mvn install
```

This will produce victims-plugin-jenkins.hpi in the target directory which can be installed through Jenkins' plugin manager.

Once the plugin is installed scanning your project becomes a new post build action available on the project configuration page.

Once your project has been built and the plugin run the log will list all vulnerable packages if any.

## Configuration options reference

The following options can be set on the configuration page of your project once the Victims post build action is added.

### baseUrl

The URL of the victims web service to used to synchronize the local database.

default: "https://victi.ms/"

### entryPoint

The entrypoint of the victims webservice to synchronize against

default: "service/"

### metadata

The severity of exception to be thrown when a dependency is encountered that matches the known vulnerable database based on metadata. Fatal indicates the build should fail, warning indicates a warning should be issued but the build should proceed.

options : warning, fatal, disabled
default : warning

### fingerprint

The severity of exception to be thrown when a dependency is encountered that matches the known vulnerable database based on a fingerprint. Fatal indicates the build should fail, warning indicates a warning should be issued but the build should proceed.

options : warning, fatal, disabled
default : warning

### updates

Allows the configuration of the synchronization mechanism. In automatic mode new entries in the victims database are pulled from the victims-web instance during each build. In daily mode new entries are pulled from the victims-web instance only once per day. The synchronization mechanism may be disabled and processed manually for closed build environments.

options : auto, offline, daily
default : auto

### jdbcDriver

The jdbc driver to use for the local victims database. By default victims uses an embedded H2 database.

default : org.h2.Driver

### jdbcUrl

The jdbc connection URL to for the local victims database.

default : .victims (embedded h2 instance).

### jdbcUser

The username to use for the jdbc connection.

default : "victims"

### jdbcPass

The password to use for the jdbc connection.

default : "victims"

### Build Directory or File

The output directory of your build or the file produced.  If a directory is supplied the plugin will recursively scan the directory for Java libraries.

### Verbose File Scanning

If set the log will list all files scanned and whether they are cached or not.
