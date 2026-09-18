package xyz.goga221.metabasis.command.group;

/** {@code /group disable <name>} */
public final class DisableCommand extends AbstractToggleCommand {

    public DisableCommand() {
        super("disable", false, "group.disable.success");
    }
}
