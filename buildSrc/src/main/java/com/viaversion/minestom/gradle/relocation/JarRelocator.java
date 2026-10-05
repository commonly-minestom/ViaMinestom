package com.viaversion.minestom.gradle.relocation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

final class JarRelocator {
    private static final String CLASS_SUFFIX = ".class";
    private static final long UNKNOWN_TIME = -1;

    private final PackageRemapper remapper;
    private final List<byte[]> residues = new ArrayList<>();

    JarRelocator(final Map<String, String> packages) {
        this.remapper = new PackageRemapper(packages);
        for (final String source : packages.keySet()) {
            residues.add(source.getBytes(StandardCharsets.UTF_8));
            residues.add(source.replace('.', '/').getBytes(StandardCharsets.UTF_8));
        }
    }

    void relocate(final File source, final File target) throws IOException {
        try (final ZipFile archive = new ZipFile(source);
             final ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(target.toPath())))) {
            final Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                transfer(archive, entries.nextElement(), output);
            }
        }
    }

    private void transfer(final ZipFile archive, final ZipEntry entry, final ZipOutputStream output) throws IOException {
        final String name = entry.getName();
        if (entry.isDirectory()) {
            write(output, name, entry.getTime(), new byte[0]);
            return;
        }

        final byte[] content;
        try (final InputStream input = archive.getInputStream(entry)) {
            content = input.readAllBytes();
        }
        if (!name.endsWith(CLASS_SUFFIX)) {
            write(output, name, entry.getTime(), content);
            return;
        }

        final ClassReader reader = new ClassReader(content);
        final ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, remapper), 0);

        final byte[] relocated = writer.toByteArray();
        requireNoResidue(name, relocated);
        write(output, relocatedName(name, reader.getClassName()), entry.getTime(), relocated);
    }

    private String relocatedName(final String name, final String className) {
        final String path = className + CLASS_SUFFIX;
        if (!name.endsWith(path)) {
            return name;
        }
        return name.substring(0, name.length() - path.length()) + remapper.map(className) + CLASS_SUFFIX;
    }

    private void requireNoResidue(final String name, final byte[] content) {
        for (final byte[] residue : residues) {
            if (contains(content, residue)) {
                throw new IllegalStateException(name + " still refers to " + new String(residue, StandardCharsets.UTF_8) + " after its relocation");
            }
        }
    }

    private void write(final ZipOutputStream output, final String name, final long time, final byte[] content) throws IOException {
        requireNoResidue(name, name.getBytes(StandardCharsets.UTF_8));

        final ZipEntry entry = new ZipEntry(name);
        if (time != UNKNOWN_TIME) {
            entry.setTime(time);
        }
        output.putNextEntry(entry);
        output.write(content);
        output.closeEntry();
    }

    private static boolean contains(final byte[] content, final byte[] pattern) {
        final int last = content.length - pattern.length;
        for (int start = 0; start <= last; start++) {
            if (matches(content, start, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(final byte[] content, final int start, final byte[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            if (content[start + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }
}
