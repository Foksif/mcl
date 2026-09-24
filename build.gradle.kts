plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "9.4.2"
    application
}

group = "me._lisik"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.13.2")
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "me._lisik.mcl.MainKt"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "me._lisik.mcl.MainKt"
    }
}