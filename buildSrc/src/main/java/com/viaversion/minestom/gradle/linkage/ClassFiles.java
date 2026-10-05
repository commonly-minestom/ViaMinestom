package com.viaversion.minestom.gradle.linkage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class ClassFiles {
    private static final String CLASS_SUFFIX = ".class";
    private static final String MODULE_DESCRIPTOR = "module-info.class";

    private ClassFiles() {
    }

    static void read(final File location, final Consumer<byte[]> consumer) throws IOException {
        if (location.isDirectory()) {
            readDirectory(location.toPath(), consumer);
        } else if (location.isFile()) {
            readArchive(location, consumer);
        }
    }

    private static void readDirectory(final Path directory, final Consumer<byte[]> consumer) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes) throws IOException {
                if (isClass(file.getFileName().toString())) {
                    consumer.accept(Files.readAllBytes(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void readArchive(final File file, final Consumer<byte[]> consumer) throws IOException {
        try (final ZipFile archive = new ZipFile(file)) {
            final Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !isClass(entry.getName())) {
                    continue;
                }
                try (final InputStream input = archive.getInputStream(entry)) {
                    consumer.accept(input.readAllBytes());
                }
            }
        }
    }

    private static boolean isClass(final String name) {
        return name.endsWith(CLASS_SUFFIX) && !name.endsWith(MODULE_DESCRIPTOR);
    }
}
