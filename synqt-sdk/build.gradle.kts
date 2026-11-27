plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

group = "com.synqt"
version = "0.1.0" // the card add-on publishes the same version and depends on this one

android {
    namespace = "com.synqt.sdk"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814") // android.jar's org.json is a stub in local unit tests
}

// Every public declaration must say so, so nothing becomes SDK API by accident.
kotlin {
    explicitApi()
    // The add-on API is this module's own; only add-ons and apps have to opt in.
    compilerOptions { optIn.add("com.synqt.sdk.addon.SynqtAddonApi") }
}

// `./gradlew :synqt-sdk:publishToMavenCentral` puts com.synqt:synqt-android on Maven Central (Central
// Portal). Credentials and the signing key come from Gradle properties or the environment
// (mavenCentralUsername, mavenCentralPassword, signingInMemoryKey, signingInMemoryKeyPassword, or
// ORG_GRADLE_PROJECT_ versions of them), never from a file in this repo.
mavenPublishing {
    configure(
        com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
            sourcesJar = com.vanniktech.maven.publish.SourcesJar.Sources(),
            variant = "release",
        ),
    )
    publishToMavenCentral()
    // Without a key (publishToMavenLocal) publish unsigned; Central refuses unsigned uploads.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates("com.synqt", "synqt-android", version.toString())
    pom {
        name = "SYNQT Android SDK"
        description = "SYNQT payins for Android apps: the payment sheet, the Payment Web App and the payin calls."
        url = "https://github.com/synqt-pay/synqt-android"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "synqt"
                name = "SYNQT"
                url = "https://synqt.com"
            }
        }
        scm {
            url = "https://github.com/synqt-pay/synqt-android"
            connection = "scm:git:git://github.com/synqt-pay/synqt-android.git"
            developerConnection = "scm:git:ssh://git@github.com/synqt-pay/synqt-android.git"
        }
    }
}
