import com.viaversion.minestom.gradle.linkage.VerifyLinkage
import com.viaversion.minestom.gradle.relocation.RelocateTransform

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

val transportPackage = "com.viaversion.minestom.transport"
val relocated = Attribute.of("com.viaversion.minestom.relocated", Boolean::class.javaObjectType)

val bundled = configurations.dependencyScope("bundled")
val bundledClasspath = configurations.resolvable("bundledClasspath") {
    extendsFrom(bundled.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(relocated, true)
    }
}

dependencies {
    artifactTypes.named(ArtifactTypeDefinition.JAR_TYPE) {
        attributes.attribute(relocated, false)
    }
    registerTransform(RelocateTransform::class) {
        from.attribute(relocated, false)
        to.attribute(relocated, true)
        parameters.packages.put("io.netty", transportPackage)
    }

    bundled(libs.viaversion)
    bundled(libs.viabackwards)

    api(libs.minestom)
    api(files(bundledClasspath))

    implementation(libs.jctools)
    implementation(libs.slf4j)
    runtimeOnly(libs.guava)
    compileOnly(libs.annotations)
}

val verifyLinkage = tasks.register<VerifyLinkage>("verifyLinkage") {
    libraries.from(bundledClasspath)
    classes.from(sourceSets.main.map { it.output.classesDirs })
    namespace.set(transportPackage)
    report.set(layout.buildDirectory.file("reports/linkage.txt"))
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing,-this-escape", "-Werror"))
    }

    jar {
        dependsOn(verifyLinkage)
        manifest.attributes("Multi-Release" to true)
        from(bundledClasspath.map { classpath -> classpath.map { zipTree(it) } }) {
            exclude("META-INF/MANIFEST.MF")
        }
    }

    check {
        dependsOn(verifyLinkage)
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
