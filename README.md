# Data Buddy

A library for assisting with the handling of many kinds of data in Minecraft Forge mods

## What's in the library?
- Helpers for defining new types of datapack data and setting up data loaders for them
- Helpers for defining Codecs for converting maps to NBT and back (with explanations on how to use Codecs)

## What can I do to use this library in my own projects?

You may bundle the jar into your projects in your favorite manner. One way of using this is to use the shadow gradle plugin in your buildscript, which helps avoid collisions with other mods that may be using the same library:

```groovy
buildscript {
	repositories {
		jcenter() // buildscript repo to get shadow from
	}
}

plugins {
	// this version works on gradle 4.9
	// more recent versions of shadow work on more recent versions of gradle
	id 'com.github.johnrengelman.shadow' version '4.0.4'
}
apply plugin: 'com.github.johnrengelman.shadow'
apply plugin: 'java' // java plugin is needed for the shadow plugin to work

repositories {
	// java repo to get databuddy from
	maven { url "https://cubicinterpolation.net/maven/" }
}

configurations {
	shade
}

dependencies {
	compile "commoble.databuddy:${databuddy_branch}:${databuddy_version}"
	shade "commoble.databuddy:${databuddy_branch}:${databuddy_version}"
	// where ${databuddy_branch} is e.g. databuddy-1.16.3
	// where ${databuddy_version} is e.g. 1.5.0.1
}

shadowJar {
	classifier = ''
	configurations = [project.configurations.shade]
	relocate 'commoble.databuddy', "${project.group}.shadow.commoble.databuddy"
}

reobf {
    shadowJar { }
}

// this replaces jar.finalizedBy('reobfJar') in the standard forge mod buildscript
tasks.build.dependsOn reobfShadowJar
jar.finalizedBy('reobfShadowJar')
```

Alternatively, many of the source classes within are relatively self-contained and can be repackaged into your own sources as-needed; please be considerate and keep the license and copyright notice in any source files you copy into your own projects in this manner.

###### The Example Mod

The sources on github include an example forge mod that uses several features in the library; the example mod is *not* built into or distributed with the library jars. Those who wish to run the example mod may run it by downloading or forking the sources from github and setting up a forge mod workspace with them.