# Open Towns Mod Template

A starter project for creating Java mods for **Open Towns** using the public Open Towns SDK.

The template handles:

- compiling against the Open Towns API
- generating `mod.properties`
- validating mod configuration
- building the mod JAR
- installing the mod
- copying XML, graphics and other traditional Towns mod content
- launching Open Towns for development

The recommended development command is:

```powershell
./gradlew dev
```

This refreshes the Open Towns API, builds and validates the mod, installs it, then launches the game.

---

# Requirements

You will need:

- Java 25
- Gradle wrapper included with this project
- a local Open Towns development checkout
- an Open Towns API compatible with the API version declared by the mod

Open Towns currently uses Java 25.

You can check your Java version with:

```powershell
java -version
```

---

# Project Structure

A typical mod looks like:

```text
my-mod/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew
├── gradlew.bat
├── gradle/
│
└── src/
    └── main/
        ├── java/
        │   └── mymod/
        │       └── MyMod.java
        │
        └── mod/
            ├── data/
            │   ├── items.xml
            │   ├── actions.xml
            │   └── ...
            │
            └── graphics/
                └── ...
```

Java source code belongs in:

```text
src/main/java
```

Traditional Towns mod content belongs in:

```text
src/main/mod
```

Everything inside `src/main/mod` is copied directly into the installed mod directory.

For example:

```text
src/main/mod/data/items.xml
```

becomes:

```text
~/.towns/mods/my-mod/data/items.xml
```

---

# Configuring the Mod

Mod configuration is stored in:

```text
gradle.properties
```

Example:

```properties
modId=myfirstmod
modName=My First Mod
modVersion=1.0.0
modAuthor=Your Name
modMainClass=myfirstmod.MyFirstMod

openTownsApiVersion=1

modDependencies=

openTownsDir=../Open-Towns
openTownsApiJar=../Open-Towns/build/libs/opentowns-api.jar
```

---

# Required Properties

The following properties are required:

```text
modId
modName
modVersion
modAuthor
modMainClass
openTownsApiVersion
openTownsApiJar
```

The build will fail with a clear error if any are missing.

---

# modId

`modId` is the permanent machine-readable identity of the mod.

Example:

```properties
modId=lava-logistics
```

Valid IDs use:

- lowercase letters
- numbers
- hyphens

Examples:

```text
strip-mine
lava-logistics
better-dwarves
automation-2
```

Avoid changing the ID after releasing a mod.

The installed mod directory uses the ID:

```text
~/.towns/mods/lava-logistics/
```

The generated JAR also uses the ID:

```text
lava-logistics.jar
```

---

# modName

`modName` is the human-readable display name.

Example:

```properties
modName=Lava Logistics
```

Unlike `modId`, the display name may safely change later.

---

# modMainClass

This must contain the complete Java package and class name for the mod entry point.

Example:

```properties
modMainClass=lavalogistics.LavaLogisticsMod
```

The validation task checks that this class actually exists inside the compiled mod JAR.

---

# Creating the Main Mod Class

The main mod class implements:

```java
TownsMod
```

A minimal mod looks like:

```java
package myfirstmod;

import xaos.api.mods.ModContext;
import xaos.api.mods.TownsMod;

public final class MyFirstMod implements TownsMod {

    @Override
    public void onLoad(ModContext context) {
        context.logger().info(
                "My First Mod loaded!"
        );
    }

    @Override
    public void onWorldLoaded(ModContext context) {
        context.logger().info(
                "World loaded!"
        );
    }

    @Override
    public void onWorldUnloaded(ModContext context) {
        context.logger().info(
                "World unloaded."
        );
    }

    @Override
    public void onUnload(ModContext context) {
        context.logger().info(
                "My First Mod unloaded."
        );
    }
}
```

---

# Mod Lifecycle

Java mods currently have four lifecycle callbacks:

```java
onLoad()
onWorldLoaded()
onWorldUnloaded()
onUnload()
```

The lifecycle is:

```text
onLoad

    ↓

onWorldLoaded

    ↓

onWorldUnloaded

    ↓

onWorldLoaded

    ↓

...

    ↓

onWorldUnloaded

    ↓

onUnload
```

`onLoad()` is called once when the Java mod is loaded.

`onWorldLoaded()` is called whenever a world becomes active.

`onWorldUnloaded()` is called when leaving that world.

`onUnload()` is called when the mod itself is unloaded, normally when Open Towns shuts down.

---

# Accessing the Open Towns API

The root API is available through:

```java
context.api()
```

For example:

```java
context.api()
        .items()
        .count(
                DefinitionId.of(
                        "my-mod",
                        "my-item"
                )
        );
```

Current API areas include:

```text
world
ui
tasks
fluids
actions
definitions
entities
events
items
buildings
inventory
production
selection
stockpiles
buildingBehaviours
itemBehaviours
```

---

# Definition IDs

Open Towns uses namespaced definition IDs.

Base game definitions use:

```text
towns:<id>
```

For example:

```text
towns:wood
towns:rmbarrel
```

Mod definitions use the mod ID as their namespace:

```text
lava-logistics:conveyor
strip-mine:marker
```

In Java:

```java
DefinitionId CONVEYOR =
        DefinitionId.of(
                "lava-logistics",
                "conveyor"
        );
```

This avoids mods accidentally treating definitions with the same short ID as belonging to the same mod.

---

# Definition Collisions

Open Towns 0.1 protects definitions from accidental cross-mod replacement.

A mod may:

```text
override a base-game definition
redefine one of its own definitions
```

A mod may not silently overwrite a definition owned by another mod.

For example:

```text
mod-a:conveyor
mod-b:conveyor
```

Both use the old internal ID:

```text
conveyor
```

The second conflicting definition will therefore be rejected rather than silently replacing the first.

Cross-mod deletion is protected in the same way.

Explicit mod-to-mod definition overrides are not part of SDK 0.1.

---

# Dependencies

Dependencies are declared using:

```properties
modDependencies=
```

For one dependency:

```properties
modDependencies=some-library
```

For several:

```properties
modDependencies=some-library,another-mod
```

Dependencies use mod IDs, not display names.

Open Towns ensures dependencies load before dependent mods.

If a dependency:

- is missing
- is disabled
- fails to load

the dependent mod will not be loaded.

Circular dependencies are rejected.

SDK 0.1 does not currently support dependency version constraints.

---

# Resources and Cleanup

World-specific registrations should normally be tracked using:

```java
context.worldResource(...)
```

For example:

```java
context.worldResource(
        context.api()
                .events()
                .subscribe(
                        SimulationTickEvent.class,
                        event -> {
                            // Handle event.
                        }
                )
);
```

Resources registered this way are automatically cleaned up when the world unloads.

Long-lived resources that should exist for the entire lifetime of the mod can use:

```java
context.resource(...)
```

Use `worldResource()` wherever possible for things created inside `onWorldLoaded()`.

This prevents duplicate event subscriptions or behaviours when changing worlds.

---

# Events

SDK 0.1 provides a small baseline event system.

Current baseline events include:

```text
SimulationTickEvent
DayStartedEvent
```

Example:

```java
context.worldResource(
        context.api()
                .events()
                .subscribe(
                        SimulationTickEvent.class,
                        event -> {
                            // Called during simulation ticks.
                        }
                )
);
```

More detailed entity creation and destruction events are planned for later SDK versions.

---

# Static XML and Assets

Java mods may also include traditional Towns XML and graphics.

Place them inside:

```text
src/main/mod
```

Example:

```text
src/main/mod/data/items.xml
src/main/mod/data/actions.xml
src/main/mod/graphics/items.png
```

During installation these are copied directly into the mod folder.

This allows a single mod to combine:

```text
Java behaviour
+
XML definitions
+
graphics
+
other Towns assets
```

---

# Building the Mod

Compile Java:

```powershell
./gradlew compileJava
```

Build the JAR:

```powershell
./gradlew jar
```

The resulting JAR is:

```text
build/libs/<modId>.jar
```

For example:

```text
build/libs/myfirstmod.jar
```

---

# Validating the Mod

Before installing or distributing a mod, run:

```powershell
./gradlew validateMod
```

Validation currently checks:

- required configuration
- valid `modId`
- API version
- dependency IDs
- duplicate dependencies
- self-dependencies
- generated JAR existence
- configured main class exists
- generated `mod.properties` exists

Successful output looks similar to:

```text
Validating My First Mod...
Mod validation passed.
  ID:           myfirstmod
  Name:         My First Mod
  Version:      1.0.0
  API version:  1
  Main class:   myfirstmod.MyFirstMod
  Dependencies: none
  JAR:          myfirstmod.jar
```

---

# Installing the Mod

Run:

```powershell
./gradlew installMod
```

or:

```powershell
./gradlew install
```

The default installation location is:

```text
C:/Users/<user>/.towns/mods/<modId>
```

For example:

```text
C:/Users/James/.towns/mods/myfirstmod
```

The install task performs a clean installation.

The previous installed directory is removed first so deleted or renamed files cannot remain from an older build.

The final directory contains approximately:

```text
myfirstmod/
├── mod.properties
├── myfirstmod.jar
├── data/
└── graphics/
```

depending on the content included by the mod.

---

# Custom Mods Directory

The mods directory can be overridden in `gradle.properties`:

```properties
townsModsDir=C:/Games/OpenTowns/mods
```

If this property is absent, the default is:

```text
<user-home>/.towns/mods
```

---

# Development Mode

The easiest development workflow is:

```powershell
./gradlew dev
```

This performs:

```text
refresh Open Towns API
        ↓
compile mod
        ↓
build JAR
        ↓
generate metadata
        ↓
validate mod
        ↓
clean previous installation
        ↓
install mod
        ↓
launch Open Towns
```

The game only launches after installation succeeds.

---

# Open Towns Development Checkout

The location of the Open Towns source checkout is configured with:

```properties
openTownsDir=../Open-Towns
```

The API JAR is configured separately:

```properties
openTownsApiJar=../Open-Towns/build/libs/opentowns-api.jar
```

When compiling the mod, the template asks the local Open Towns checkout to run:

```text
apiJar
```

This prevents development mods accidentally compiling against an outdated API JAR.

Gradle will skip unnecessary work when the API is already up to date.

---

# Generated mod.properties

You should not normally edit `mod.properties` manually.

The Gradle build generates it from `gradle.properties`.

Example output:

```properties
id=myfirstmod
name=My First Mod
version=1.0.0
author=Your Name
apiVersion=1
dependencies=
jar=myfirstmod.jar
main=myfirstmod.MyFirstMod
```

The generated file is stored under:

```text
build/generated/modMetadata/mod.properties
```

and copied into the installed mod automatically.

Gradle tracks the metadata properties as task inputs, so changing the mod version, dependencies or other metadata regenerates the file.

---

# API Version

Mods declare the API version they were built against:

```properties
openTownsApiVersion=1
```

Open Towns checks this when loading the mod.

A mod requiring a newer unsupported API version will be rejected cleanly rather than attempting to load against an incompatible SDK.

SDK version and API version are separate concepts.

For example:

```text
SDK release: 0.1.0
API version: 1
```

---

# Save Data

Mods can store small amounts of persistent data using:

```java
context.saveData()
```

Each mod receives its own save-data area.

Save data is stored alongside the Open Towns save under the mod's namespace, preventing one mod from overwriting another mod's data.

Avoid depending directly on another mod's save data.

---

# Logging

Use the logger provided by the context:

```java
context.logger().info(
        "Something happened."
);
```

Log messages are automatically labelled with the mod ID:

```text
[Mod:myfirstmod] Something happened.
```

This makes bug reports involving multiple mods easier to diagnose.

---

# Recommended Development Workflow

For normal development:

```powershell
./gradlew dev
```

For a quick compile check:

```powershell
./gradlew compileJava
```

Before committing or distributing:

```powershell
./gradlew validateMod
```

To install without launching:

```powershell
./gradlew installMod
```

---

# Troubleshooting

## Open Towns API JAR was not found

Check:

```properties
openTownsDir=...
openTownsApiJar=...
```

The API JAR should normally exist at:

```text
<Open-Towns>/build/libs/opentowns-api.jar
```

You can build it directly from the Open Towns repository with:

```powershell
./gradlew apiJar
```

---

## Main class not found

If validation reports:

```text
Configured mod main class was not found inside the JAR
```

check that:

```properties
modMainClass=my.package.MyMod
```

matches both the Java package:

```java
package my.package;
```

and class:

```java
public final class MyMod implements TownsMod
```

---

## Mod does not appear in Open Towns

Run:

```powershell
./gradlew installMod
```

Then check:

```text
~/.towns/mods/<modId>/
```

contains:

```text
mod.properties
<modId>.jar
```

Also inspect the Open Towns log for messages from:

```text
JavaModLoader
```

---

## Dependency failed to load

Check:

```properties
modDependencies=
```

Dependency entries must use the target mod's `modId`.

Example:

```properties
modDependencies=some-library
```

not:

```properties
modDependencies=Some Library
```

---

# SDK 0.1 Scope

Open Towns SDK 0.1 is a developer preview.

The initial goal is to provide a stable foundation for Java mods rather than expose every part of the game immediately.

The 0.1 SDK includes foundations for:

- Java mod loading
- lifecycle callbacks
- dependency ordering
- API version checking
- namespaced definitions
- definition ownership
- events
- tasks
- actions
- items
- buildings
- inventory
- reservations
- production
- fluids
- selection
- behaviours
- per-mod save data
- resource cleanup

Future SDK releases are expected to expand areas such as:

- UI APIs
- richer game events
- additional world manipulation
- explicit mod-to-mod definition overrides
- dependency version constraints
- additional mod development tooling

---

# Stability

SDK 0.1 should be treated as an early developer API.

Where possible, mods should use classes under:

```text
xaos.api.*
```

and must not rely on Open Towns engine implementation classes under:

```text
xaos.internal.*
```

Internal classes are not part of the supported modding API and may change without compatibility guarantees.