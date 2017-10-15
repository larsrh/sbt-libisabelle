# sbt-libisabelle

| Service                   | Status |
| ------------------------- | ------ |
| Travis (Linux/Mac CI)     | [![Build Status](https://travis-ci.org/larsrh/sbt-libisabelle.svg?branch=master)](https://travis-ci.org/larsrh/sbt-libisabelle) |
| AppVeyor (Windows CI)     | [![Build status](https://ci.appveyor.com/api/projects/status/upnd09ldkgnu8b0d/branch/master?svg=true)](https://ci.appveyor.com/project/larsrh/sbt-libisabelle/branch/master) |
| Gitter (Chat)             | [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/larsrh/libisabelle) |


## Usage

In your `project/plugins.sbt`, add this line:

```scala
// sbt 0.13.x
addSbtPlugin("info.hupel" % "sbt-libisabelle" % "0.5.1")

// sbt 1.0.x
addSbtPlugin("info.hupel" % "sbt-libisabelle" % "0.6.0")
```

To enable the plugin for a build, add this to your `build.sbt`:

```scala
enablePlugins(LibisabellePlugin)
```

Now, you can configure the following keys:

```scala
isabelleVersions := Seq("2016", "2016-1")
isabelleSessions in Compile := Seq("Example")
```

If unset, `isabelleVersions` defaults to the content of the environment variable `ISABELLE_VERSION`.

This allows you to put Isabelle source files (including `ROOT`) into `src/main/isabelle`.
In the sbt shell, you can type `isabelleBuild` to build the `Example` session for both Isabelle2016 and Isabelle2016-1.
Furthermore, you can use `isabelleJEdit Example 2016` to open a jEdit instance with the `Example` session loaded.

The plugin also allows you to `package` and `publish` artifacts containing Isabelle sources as usual JAR files (with some metadata added).

Note that this plugin pulls in a dependency to [sbt-assembly](https://github.com/sbt/sbt-assembly) and configures a setting due to the way Isabelle artifacts are packaged.

## Combined usage with libisabelle

The plugin does not add a compile-time dependency on [libisabelle](https://github.com/larsrh/libisabelle) to your project.
It only allows you to control Isabelle from within sbt.
In case you want to use libisabelle's features (most likely you want to do that), you need to add this as a `libraryDependency` yourself.
Look below for compatible versions.

You may also want to check out [isabelle-irc-bot](https://github.com/larsrh/isabelle-irc-bot), which contains a full-blown demo project using both libisabelle and sbt-libisabelle.

## Compatibility matrix

| sbt-libisabelle version  | sbt versions  | libisabelle version | Isabelle versions           |
| ------------------------ | ------------- | ------------------- | --------------------------- |
| 0.4.x                    | 0.13.x        | 0.6.x               | 2016                        |
| 0.5.0                    | 0.13.x        | 0.7.x – 0.9.x       | 2016, 2016-1                |
| 0.5.1                    | 0.13.x, 1.0.x | 0.7.x – 0.9.x       | 2016, 2016-1                |
| 0.6.0                    | 1.0.x         | 0.7.x – 0.9.x       | 2016, 2016-1, Generic       |
| 0.6.1                    | 1.0.x         | 0.7.x – 0.9.x       | 2016, 2016-1, 2017, Generic |

_Generic_ means that arbitrary versions are supported (not on Windows), as long as they have a `bin/isabelle` executable.
This is tested on Linux and macOS and should work with Isabelle since at least 2013.
