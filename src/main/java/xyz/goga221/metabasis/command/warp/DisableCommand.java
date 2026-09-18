package xyz.goga221.metabasis.command.warp;

/** {@code /warp disable <name>} */
public final class DisableCommand extends AbstractToggleCommand {

    public DisableCommand() {
        super("disable", false, "warp.disable.success");
    }
}
