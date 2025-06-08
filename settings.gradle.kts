pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal {
			content {
				excludeGroup("org.apache.logging.log4j")
			}
		}
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
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "not-a-manual-anvil"