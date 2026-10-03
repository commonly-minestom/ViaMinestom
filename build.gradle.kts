plugins {
    `java-library`
    `maven-publish`
}

group = "com.viaversion"
version = property("projectVersion") as String
description = "Runs ViaVersion, ViaBackwards and ViaRewind inside a Minestom server."

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

dependencies {
    api(libs.minestom)
    api(libs.viaversion)
    api(libs.viabackwards)
    api(libs.viarewind)
    api(libs.bundles.netty)

    implementation(libs.jctools)
    implementation(libs.guava)
    implementation(libs.slf4j)
    compileOnly(libs.annotations)

    for (classifier in listOf("linux-x86_64", "linux-aarch_64")) {
        runtimeOnly(variantOf(libs.nettyNativeEpoll) { classifier(classifier) })
    }
    for (classifier in listOf("osx-x86_64", "osx-aarch_64")) {
        runtimeOnly(variantOf(libs.nettyNativeKqueue) { classifier(classifier) })
    }
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
