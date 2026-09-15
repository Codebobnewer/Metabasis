package xyz.goga221.metabasis.gui.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shows a Paper Dialog with one or more text fields (the admin GUI's text-entry mechanism, per
 * the client's requirement to use Paper/Bukkit's Dialog API rather than a chat-prompt hack) and
 * forwards whatever the player submits to a callback.
 */
public final class TextInputDialogs {

    private static final int FIELD_WIDTH = 250;

    private TextInputDialogs() {
    }

    /** One text field: {@code key} is how its value is read back out of the submitted map. */
    public record Field(String key, String label, String initial) {
    }

    /** A single-field prompt — the common case (rebind a permission, edit a description, ...). */
    public static void promptText(Player player, String title, String label, String initial, Consumer<String> onSubmit) {
        prompt(player, title, List.of(new Field("value", label, initial)),
                values -> onSubmit.accept(values.get("value")));
    }

    /**
     * A multi-field prompt (e.g. creating a group: name + permission + description at once).
     * Blank fields come back as {@code null} in the map, so callers can treat that the same way
     * the equivalent command's optional arguments already do.
     */
    public static void prompt(Player player, String title, List<Field> fields, Consumer<Map<String, String>> onSubmit) {
        List<TextDialogInput> inputs = fields.stream()
                .map(field -> DialogInput.text(field.key(), Component.text(field.label()))
                        .initial(field.initial() == null ? "" : field.initial())
                        .width(FIELD_WIDTH)
                        .build())
                .toList();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text(title))
                        .inputs(inputs)
                        .build())
                .type(DialogType.notice(
                        ActionButton.builder(Component.text("Submit"))
                                .action(DialogAction.customClick(
                                        (view, audience) -> {
                                            Map<String, String> values = new LinkedHashMap<>();
                                            for (Field field : fields) {
                                                String value = view.getText(field.key());
                                                values.put(field.key(), value == null || value.isBlank() ? null : value);
                                            }
                                            onSubmit.accept(values);
                                        },
                                        ClickCallback.Options.builder().build()))
                                .build())));

        player.showDialog(dialog);
    }
}
