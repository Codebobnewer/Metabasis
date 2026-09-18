package xyz.goga221.metabasis.command.warp;

/** {@code /warp enable <name>} */
public final class EnableCommand extends AbstractToggleCommand {

    public EnableCommand() {
        super("enable", true, "warp.enable.success");
    }
}
