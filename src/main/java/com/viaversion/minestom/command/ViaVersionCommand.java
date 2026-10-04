package com.viaversion.minestom.command;

import com.viaversion.viaversion.commands.ViaCommandHandler;
import java.util.Arrays;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;

public final class ViaVersionCommand extends Command {
    private static final String PERMISSION = "viaversion.command";
    private static final String[] NO_ARGUMENTS = new String[0];

    private final ViaCommandHandler handler;
    private final CommandAuthorizer authorizer;

    public ViaVersionCommand(final ViaCommandHandler handler, final CommandAuthorizer authorizer) {
        super("viaversion", "viaver", "vvminestom");
        this.handler = handler;
        this.authorizer = authorizer;

        final ArgumentStringArray arguments = ArgumentType.StringArray("arguments");
        arguments.setSuggestionCallback(this::suggest);

        setCondition((sender, command) -> authorizer.isAuthorized(sender, PERMISSION));
        setDefaultExecutor((sender, context) -> handler.onCommand(wrap(sender), NO_ARGUMENTS));
        addSyntax((sender, context) -> handler.onCommand(wrap(sender), context.get(arguments)), arguments);
    }

    private void suggest(final CommandSender sender, final CommandContext context, final Suggestion suggestion) {
        final String[] tokens = context.getInput().replace("\0", "").split(" ", -1);
        final String[] arguments = Arrays.copyOfRange(tokens, 1, tokens.length);
        for (final String completion : handler.onTabComplete(wrap(sender), arguments)) {
            suggestion.addEntry(new SuggestionEntry(completion));
        }
    }

    private MinestomCommandSender wrap(final CommandSender sender) {
        return new MinestomCommandSender(sender, authorizer);
    }
}
