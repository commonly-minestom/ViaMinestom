package com.viaversion.minestom.gradle.relocation;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;

@CacheableTransform
public abstract class RelocateTransform implements TransformAction<RelocateTransform.Parameters> {

    public interface Parameters extends TransformParameters {

        @Input
        MapProperty<String, String> getPackages();
    }

    @InputArtifact
    @Classpath
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(final TransformOutputs outputs) {
        final File source = getInputArtifact().get().getAsFile();
        final File target = outputs.file(source.getName());
        try {
            new JarRelocator(getParameters().getPackages().get()).relocate(source, target);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to relocate " + source.getName(), e);
        }
    }
}
