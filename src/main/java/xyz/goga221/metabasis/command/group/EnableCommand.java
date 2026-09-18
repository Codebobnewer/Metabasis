package xyz.goga221.metabasis.command.group;

/** {@code /group enable <name>} */
public final class EnableCommand extends AbstractToggleCommand {

    public EnableCommand() {
        super("enable", true, "group.enable.success");
    }
}
