package xyz.goga221.metabasis.command;

import dev.jorel.commandapi.CommandAPICommand;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Scans a leaf-command package (e.g. {@code command.warp}) and builds the {@link CommandAPICommand} for every concrete {@link BaseCommand} it finds. */
public final class CommandGroupScanner {

    private CommandGroupScanner() {
    }

    public static List<CommandAPICommand> scan(String packageName) {
        List<CommandAPICommand> subcommands = new ArrayList<>();
        try {
            for (Class<?> clazz : ClassScanner.getClassesInCurrentJar(packageName)) {
                if (BaseCommand.class.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                    BaseCommand command = (BaseCommand) clazz.getDeclaredConstructor().newInstance();
                    subcommands.add(command.getCommand());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan command package: " + packageName, e);
        }
        return subcommands;
    }
}
