plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktechMavenPublish)
}

group = "io.github.jdemeulenaere"

version = providers.gradleProperty("compose.driver.version").get()

kotlin {
    jvm()
    androidLibrary {
        namespace = "io.github.jdemeulenaere.compose.driver"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.androidx.navigation.event)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.call.logging)
                implementation(libs.ktor.server.status.pages)
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(compose.foundation)
                runtimeOnly(compose.desktop.currentOs)
            }
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "compose-driver",
        version = project.version.toString(),
    )

    pom {
        name = "Compose Driver"
        description = "Make AI tools able to see and control Compose UIs"
        url = "https://github.com/jdemeulenaere/compose-driver"
        inceptionYear = "2026"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "jdemeulenaere"
                name = "Jordan Demeulenaere"
                url = "https://github.com/jdemeulenaere/"
            }
        }
        scm {
            connection = "scm:git:git://github.com/jdemeulenaere/compose-driver.git"
            developerConnection = "scm:git:ssh://github.com/jdemeulenaere/compose-driver.git"
            url = "https://github.com/jdemeulenaere/compose-driver"
        }
    }

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
