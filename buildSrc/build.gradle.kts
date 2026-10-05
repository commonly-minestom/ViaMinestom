plugins {
    `java-library`
}

dependencies {
    implementation(gradleApi())
    implementation(libs.asmCommons)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
        options.compilerArgs.addAll(listOf("-Xlint:all,-classfile,-processing", "-Werror"))
    }
}
