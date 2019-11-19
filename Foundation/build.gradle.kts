/**
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.nemerosa.versioning.SCMInfo
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "1.3.50"

plugins {
    kotlin("jvm") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "4.0.1"
    id("net.nemerosa.versioning") version "2.8.2"
}

versioning {
    dirty = KotlinClosure1<String, String>({ this })
    full = KotlinClosure1<SCMInfo, String>({ "${this.lastTag}-${this.branch}-${this.abbreviated}" })
}

group = "com.mypackage.foundation"
version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {

    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

   //Configuration
    api("com.typesafe:config:1.3.3")
    implementation("com.google.code.gson:gson:2.8.5")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.7.6")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
    implementation("org.apache.logging.log4j:log4j-core:2.12.1")

    // Application
    implementation("com.fasterxml.uuid:java-uuid-generator:3.2.0")
    implementation("com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers:1.1.1")
    implementation("com.google.cloud:google-cloud-storage:1.96.0")
    implementation("com.google.cloud:google-cloud-pubsub:1.96.0")

    // Testing
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf("-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer")
}

tasks.withType<ShadowJar> {
    version = version
    manifest.attributes.apply {
        put("Implementation-Title", "MyPackage Foundation")
        put("Implementation-Version", version)
    }
    // To fix: https://github.com/googleapis/google-cloud-java/issues/4700
    mergeServiceFiles {
        setPath("META-INF/services")
        include("io.grpc.*")
    }
}
