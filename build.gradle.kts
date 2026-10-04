plugins {
    `java-library`
    `maven-publish`
}

group = "com.viaversion"
version = property("projectVersion") as String
description = "Runs ViaVersion and ViaBackwards inside a Minestom server."

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

dependencies {
    api(libs.minestom)
    api(libs.viaversion)
    api(libs.viabackwards)
    api(libs.bundles.viaversionApi)

    implementation(libs.jctools)
    implementation(libs.slf4j)
    runtimeOnly(libs.guava)
    compileOnly(libs.annotations)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing,-this-escape", "-Werror"))
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
