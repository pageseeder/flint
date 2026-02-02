import java.time.Instant

plugins {
  id("stable-versions") apply false
  alias(libs.plugins.jreleaser)
  war
}

val theVersion = File("$rootDir/version.txt").readText(Charsets.UTF_8).trim()
val gitName = "flint"

// Global version
version = theVersion

// This block is evaluated BEFORE the individual projects
allprojects {

  // Project properties
  group = "org.pageseeder.flint"
  version = theVersion

  // Dependencies common to all projects
  repositories {
    mavenGitlab(project)
    mavenCentral()
    mavenLocal()
  }
}

// This block is evaluated BEFORE the individual projects
subprojects {

  apply(plugin = "java-library")
  apply(plugin = "stable-versions")
  apply(plugin = "maven-publish")

  dependencies {
    implementation(rootProject.libs.xmlwriter)
    implementation(rootProject.libs.slf4j.api)
    runtimeOnly(rootProject.libs.saxon)
    testImplementation(rootProject.libs.junit)
  }

  // Enforce Java 11
  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
  }

  // Common Jar properties
  tasks.getByName<Jar>("jar") {
    manifest {
      attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to theVersion,
        "Implementation-Vendor" to "Allette Systems (Australia) Pty Ltd",
        "Build-Timestamp" to Instant.now(),
        "Created-By" to "Gradle ${gradle.gradleVersion}",
        "Built-By" to System.getProperty("user.name"),
        "Build-Jdk" to System.getProperty("java.version")
      )
    }
  }

  extensions.configure<PublishingExtension>("publishing") {
    // Add the staging deploy repository
    repositories {
      maven {
        url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
      }
    }
    publications {
      create<MavenPublication>("mavenJava") {
        from(components["java"])
        pom {
          name.set(project.name)
          description.set(project.description)
          url.set("https://github.com/weborganic/flint")
          organization {
            name.set("Allette Systems")
            url.set("https://www.allette.com.au")
          }
          licenses {
            license {
              name.set("The Apache Software License, Version 2.0")
              url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }
          scm {
            url.set("git@github.com:pageseeder/${gitName}.git")
            connection.set("scm:git:git@github.com:pageseeder/${gitName}.git")
            developerConnection.set("scm:git:git@github.com:pageseeder/${gitName}.git")
          }
          developers {
            developer {
              name.set("Christophe Lauret")
              email.set("clauret@weborganic.com")
            }
            developer {
              name.set("Jean-Baptiste Reure")
              email.set("jbreure@weborganic.com")
            }
          }
        }
      }
    }
  }

}

// Set Gradle version
tasks.wrapper {
  gradleVersion = "8.5"
  distributionType = Wrapper.DistributionType.ALL
}


jreleaser {
  configFile.set(file("jreleaser.toml"))

  // subproject distributions
  distributions {
    subprojects.forEach { subproject ->
      register(subproject.name) {
        artifact {
          path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}.jar"))
        }
        artifact {
          path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}-sources.jar"))
        }
        artifact {
          path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}-javadoc.jar"))
        }
      }
    }
  }
}