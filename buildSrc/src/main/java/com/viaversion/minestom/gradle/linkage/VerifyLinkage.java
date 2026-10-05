package com.viaversion.minestom.gradle.linkage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;

@CacheableTask
public abstract class VerifyLinkage extends DefaultTask {

    @Classpath
    public abstract ConfigurableFileCollection getLibraries();

    @Classpath
    public abstract ConfigurableFileCollection getClasses();

    @Input
    public abstract Property<String> getNamespace();

    @Input
    public String getPlatformVersion() {
        return Runtime.version().toString();
    }

    @OutputFile
    public abstract RegularFileProperty getReport();

    @TaskAction
    public void verify() throws IOException {
        final String namespace = getNamespace().get();
        final ClassIndex index = new ClassIndex();
        final List<byte[]> subjects = new ArrayList<>();
        for (final File library : getLibraries()) {
            ClassFiles.read(library, content -> {
                index.add(content);
                subjects.add(content);
            });
        }
        for (final File location : getClasses()) {
            ClassFiles.read(location, index::add);
        }

        final TypeGraph types = new TypeGraph(index, namespace.replace('.', '/') + '/');
        final Violations violations = new Violations();
        final ReferenceVerifier references = new ReferenceVerifier(types, violations);
        final InheritanceVerifier inheritance = new InheritanceVerifier(types, violations);
        for (final byte[] subject : subjects) {
            final ClassReader reader = new ClassReader(subject);
            references.verify(reader);
            inheritance.verify(index.find(reader.getClassName()));
        }

        final boolean linked = references.verified() > 0 && violations.isEmpty();
        final String report = describe(namespace, references.verified(), violations);
        Files.writeString(getReport().get().getAsFile().toPath(), report);
        if (!linked) {
            throw new GradleException(report);
        }
    }

    private static String describe(final String namespace, final int verified, final Violations violations) {
        if (verified == 0) {
            return "The libraries do not refer to " + namespace + " at all";
        }
        if (violations.isEmpty()) {
            return "All " + verified + " references to " + namespace + " resolve";
        }
        return violations.size() + " requirements on " + namespace + " are not met:" + violations.describe();
    }
}
