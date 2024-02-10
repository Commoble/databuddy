# Data Buddy

A library for assisting with the handling of many kinds of data in Minecraft Neoforge mods

## What's in the library?
- Helpers for defining new types of datapack data and setting up data loaders for them
- Helpers for defining Codecs for converting maps to NBT and back (with explanations on how to use Codecs)
- Helpers for setting up configs and subscribing them to config reload events
- Helpers for creating annotation-based plugin loaders

## What can I do to use this library in my own projects?

You may bundle the jar into your projects in your favorite manner. One way of using this is to use the shadow gradle plugin in your buildscript, which helps avoid collisions with other mods that may be using the same library:

```groovy
plugins {
    // this version works on gradle 8.x
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}
apply plugin: 'com.github.johnrengelman.shadow'
apply plugin: 'java' // java plugin is needed for the shadow plugin to work

repositories {
	// java repo to get databuddy from
	maven { url "https://commoble.net/maven/" }
}

configurations {
	shade
}

dependencies {
	implementation "net.commoble.databuddy:${databuddy_branch}:${databuddy_version}"
	shade "net.commoble.databuddy:${databuddy_branch}:${databuddy_version}"
	// where ${databuddy_branch} is e.g. databuddy-1.20.4
	// where ${databuddy_version} is e.g. 5.0.0.0
}

shadowJar {
	classifier = ''
	configurations = [project.configurations.shade]
	relocate 'net.commoble.databuddy', "${project.group}.shadow.net.commoble.databuddy"
}

tasks.build.dependsOn shadowJar
jar.finalizedBy('shadowJar')
```

Alternatively, many of the source classes within are relatively self-contained and can be repackaged into your own sources as-needed; please be considerate and keep the license and copyright notice in any source files you copy into your own projects in this manner.

###### The Example Mod

The sources on github include an example neoforge mod that uses several features in the library; the example mod is *not* built into or distributed with the library jars. Those who wish to run the example mod may run it by downloading or forking the sources from github and setting up a neoforge mod workspace with them.