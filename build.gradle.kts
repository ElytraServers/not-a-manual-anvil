import groovy.lang.Closure

plugins {
	java
	id("xyz.wagyourtail.unimined") version "1.2.6"
	id("com.palantir.git-version") version "3.1.0"

	id("com.diffplug.spotless") version "7.0.4"
}

val gitVersion: Closure<String> by extra

group = "cn.taskeren"
version = getCurrentVersion()

val modCompileOnly: Configuration by configurations.creating
configurations.compileOnly.get().extendsFrom(modCompileOnly)

unimined.minecraft {
	version("1.20.1")
	side("combined")

	mappings {
		mojmap()
	}

	minecraftForge {
		loader("47.2.6")
		mixinConfig("not_a_manual_anvil.mixins.json")
	}

	mods {
		remap(modCompileOnly) {
		}

		modImplementation {
			catchAWNamespaceAssertion()
		}
	}
}

val modImplementation by configurations.getting

base {
	archivesName = "not_a_manual_anvil+1.20.1"
}

repositories {
	mavenCentral()
	maven {
		name = "CurseMaven"
		url = uri("https://cursemaven.com")
	}
	maven {
		name = "Blamejared"
		url = uri("https://maven.blamejared.com")
	}
}

dependencies {
	val tfcProjectId = "302973"
	val tfcVersionId = "6187491" // 1.20.1-3.2.14
	modImplementation("curse.maven:TerraFirmaCraft-${tfcProjectId}:${tfcVersionId}")
	modImplementation("vazkii.patchouli:Patchouli:1.20.1-84.1-FORGE")
}

tasks.test {
	useJUnitPlatform()
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

tasks.processResources {
	inputs.property("version", project.version)

	val files = listOf("META-INF/mods.toml")
	filesMatching(files) {
		expand("version" to project.version)
	}
}

fun getCurrentVersion(): String {
	val envVersion = System.getenv("VERSION")
	if(envVersion != null) return envVersion

	return try {
		gitVersion()
	} catch(e: Exception) {
		println("Failed to get git version: ")
		e.printStackTrace()
		"NO-GIT-TAG-SET"
	}
}

spotless {
	java {
		toggleOffOn()

		importOrder()
		removeUnusedImports()

		val eclipseFormatPreference = file("gradle/spotless.eclipseformat.xml")
		if(!eclipseFormatPreference.exists()) {
			error("Eclipse format preference file not found: ${eclipseFormatPreference.absolutePath}")
		}

		eclipse("4.19.0").configFile(eclipseFormatPreference)
	}
}