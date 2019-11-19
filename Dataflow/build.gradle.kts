import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.nemerosa.versioning.SCMInfo
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Set the project appropriately (this should be much more elegant using `val` and
// pattern matching but it triggers a kotlin compiler bug
val pipeline = System.getProperty("pipeline") ?: "no-pipeline"
println("Configured pipeline: $pipeline")

var mainClass = ""
var baseName = ""

if(pipeline.equals("gcs2bigquery")) {
    mainClass = "com.mypackage.data.poc.Gcs2BqPipeline"
    baseName = "gcs2bq-pipeline"
} else {
    mainClass = "com.mypackage.data.poc.DataflowPipeline"
    baseName = "dataflow-pipeline"
}

println("Main Class: $mainClass, Base Name: $baseName")

plugins {
    application
    kotlin("jvm") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "4.0.1"
    id("net.nemerosa.versioning") version "2.8.2"
}

versioning {
    dirty = KotlinClosure1<String, String>({ this })
    full = KotlinClosure1<SCMInfo, String>({ "${this.lastTag}-${this.branch}-${this.abbreviated}" })
}

application {
    group = "com.mypackage.data.poc"
    version = versioning.info.full
    mainClassName = mainClass
}

repositories {
    mavenCentral()
    jcenter()
}

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

dependencies {

    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.50")

    //Configuration
    api("com.typesafe:config:1.3.3")
    implementation("com.google.code.gson:gson:2.8.5")

    // Foundation
    implementation("com.mypackage.foundation:foundation")

    // Apache Beam
    implementation("org.apache.beam:beam-sdks-java-core:2.15.0")
    implementation("org.apache.beam:beam-sdks-java-io-kafka:2.15.0") {
        exclude (group = "org.apache.kafka", module = "kafka-clients")
    }
    implementation("org.apache.beam:beam-runners-direct-java:2.15.0")
    implementation("org.apache.beam:beam-runners-google-cloud-dataflow-java:2.15.0")
    // Logging
    implementation("io.github.microutils:kotlin-logging:1.7.6")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
    implementation("org.apache.logging.log4j:log4j-core:2.12.1")

    // Application
    implementation("io.javalin:javalin:3.5.0")
    implementation("org.apache.kafka:kafka-clients:2.3.0") {
        isForce = true // Force this version of the Kafka client as the Beam SDK relies on 1.0
    }
    implementation("com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers:1.1.1")

    // Testing
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.0")

}

tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.withType<KotlinCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    kotlinOptions.jvmTarget = "1.8" // Java 8 needed as Beam doesn't yet support 11
}

tasks.withType<ShadowJar> {
    manifest.attributes.apply {
        baseName = baseName
        classifier = ""
        version = version
        isZip64 = true
        put("Implementation-Title", "PoC DataFlow Pipeline")
        put("Implementation-Version", version)
        put("Main-Class", mainClass)
    }
}

