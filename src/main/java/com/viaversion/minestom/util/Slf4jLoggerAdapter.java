package com.viaversion.minestom.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class Slf4jLoggerAdapter extends Logger {
    private final org.slf4j.Logger delegate;

    public Slf4jLoggerAdapter(final org.slf4j.Logger delegate) {
        super(delegate.getName(), null);
        this.delegate = delegate;
        setLevel(Level.ALL);
    }

    @Override
    public void log(final LogRecord record) {
        final String message = format(record);
        final Throwable thrown = record.getThrown();
        final int level = record.getLevel().intValue();
        if (level >= Level.SEVERE.intValue()) {
            delegate.error(message, thrown);
        } else if (level >= Level.WARNING.intValue()) {
            delegate.warn(message, thrown);
        } else if (level >= Level.INFO.intValue()) {
            delegate.info(message, thrown);
        } else if (level >= Level.FINE.intValue()) {
            delegate.debug(message, thrown);
        } else {
            delegate.trace(message, thrown);
        }
    }

    private static String format(final LogRecord record) {
        final String message = record.getMessage();
        final Object[] parameters = record.getParameters();
        if (message == null || parameters == null || parameters.length == 0) {
            return message;
        }
        try {
            return MessageFormat.format(message, parameters);
        } catch (final IllegalArgumentException e) {
            return message;
        }
    }
}
