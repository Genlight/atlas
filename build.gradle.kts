plugins {
    java
    application
    antlr

    id("com.diffplug.gradle.spotless") version "3.16.0"
    id("com.gradle.build-scan") version "2.2.1"
}

repositories {
    jcenter()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

dependencies {
    // We need to give the ANTLR Plugin a hint.
    antlr("org.antlr:antlr4:4.7.1")
    implementation("org.antlr:antlr4:4.7.1")

    implementation("com.google.guava:guava:26.0-jre")

    implementation("org.apache.commons:commons-text:1.6")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:2.11.1")
    implementation("org.apache.logging.log4j:log4j-core:2.11.1")

    // Lombok
    compileClasspath("org.projectlombok:lombok:1.18.4")
    annotationProcessor("org.projectlombok:lombok:1.18.4")

    // Maths
    implementation("org.hipparchus:hipparchus-core:1.4")

    // Commandline Parameters
    implementation("info.picocli:picocli:4.0.0-alpha-2")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.3.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.2")
}

application {
    mainClassName = "xyz.leutgeb.lorenz.logs.Main"
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(arrayOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

tasks.withType<AntlrTask> {
    // See https://github.com/antlr/antlr4/blob/master/doc/tool-options.md
    arguments.addAll(arrayOf(
            "-visitor",
            "-no-listener",
            "-long-messages",
            "-package", "xyz.leutgeb.lorenz.logs.antlr",
            "-Werror",
            "-Xlog",
            "-lib", "src/main/antlr/xyz/leutgeb/lorenz/logs/antlr"
    ))
}

/*
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
*/

tasks.withType<Wrapper> {
    gradleVersion = "5.3"
    distributionType = Wrapper.DistributionType.ALL
}

// Aggresively format code when building.
tasks["build"].dependsOn("spotlessJavaApply")

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
    if (System.getenv("CI") != null) {
        publishAlways()
        tag("ci")
        link("Travis CI", System.getenv("TRAVIS_JOB_WEB_URL"))
    }
}

spotless {
    java {
        // Please do not add any custom configuration here.
        // We just bow and abide to Google's rules,
        // trading off individualism for simplicity.
        googleJavaFormat()
        // Explicitly point gjf at src, otherwise it will also check build and find ANTLR generated code.
        target("src/**/*.java")
    }
    kotlinGradle {
        ktlint()

        target("build.gradle.kts")
    }
}
