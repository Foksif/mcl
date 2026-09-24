plugins {
    kotlin("jvm") version "2.3.20"
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

tasks.test {
    useJUnitPlatform()
}