pluginManagement {
	repositories {
		maven {
			url = uri("https://maven.wagyourtail.xyz/releases")
		}
		maven {
			url = uri("https://maven.wagyourtail.xyz/snapshots")
		}
		maven {
			name = "SpongePowered Maven"
			url = uri("https://repo.spongepowered.org/repository/maven-public/")
		}
		mavenCentral() // highly recommended, but not required
		gradlePluginPortal {
			content {
				// this is not required either, unless jcenter goes down again, then it might fix things
				excludeGroup("org.apache.logging.log4j")
			}
		}
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "not-a-manual-anvil"