// Aggregator only — every source file lives in :api or :core, and :core produces the plugin jar.

allprojects {
    group = "it.yteicos"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "nexo"
            url = uri("https://repo.nexomc.com/releases")
        }
        maven {
            // MMOItems + MythicLib. The old repo.lumine.io host is gone.
            name = "phoenix"
            url = uri("https://nexus.phoenixdevt.fr/repository/maven-public/")
        }
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    dependencies {
        "compileOnly"("io.papermc.paper:paper-api:26.2.build.+")
        "testImplementation"(platform("org.junit:junit-bom:5.10.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
