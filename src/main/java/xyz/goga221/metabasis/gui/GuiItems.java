package xyz.goga221.metabasis.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

import java.util.Arrays;

/** Small shared helper so every GUI screen builds items the same way, from Adventure Components. */
final class GuiItems {

    private GuiItems() {
    }

    static ItemProvider of(Material material, Component name, Component... lore) {
        ItemBuilder builder = new ItemBuilder(material).setDisplayName(new AdventureComponentWrapper(name));
        if (lore.length > 0) {
            builder.addLoreLines(Arrays.stream(lore)
                    .map(AdventureComponentWrapper::new)
                    .toArray(AdventureComponentWrapper[]::new));
        }
        return builder;
    }
}
