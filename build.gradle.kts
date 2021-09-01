plugins {

    `java-gradle-plugin`

    kotlin("jvm") version "1.5.21"

    `kotlin-dsl`

    id("com.gradle.plugin-publish") version "0.15.0"

    `maven-publish`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

group = "de.undercouch"
version = "4.1.2"

dependencies {

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation(kotlin("stdlib-jdk8"))

    // Use the Kotlin test library.
    testImplementation(kotlin("test"))

    // Use the Kotlin JUnit integration.
//    testImplementation(kotlin("test-junit"))

    implementation("org.apache.httpcomponents:httpclient:4.5.3")

    testImplementation("com.github.tomakehurst:wiremock-jre8:2.31.0-SNAPSHOT") {
        // https://github.com/tomakehurst/wiremock/issues/684#issuecomment-908986780
        exclude(group = "org.junit", module = "junit-bom")
    }

}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    test { useJUnitPlatform() }
}
