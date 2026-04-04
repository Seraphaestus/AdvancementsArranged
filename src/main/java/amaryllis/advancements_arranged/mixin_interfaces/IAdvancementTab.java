package amaryllis.advancements_arranged.mixin_interfaces;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;

import java.util.Map;

public interface IAdvancementTab {
    Map<AdvancementHolder, AdvancementWidget> getWidgets();
    AdvancementWidget arranged$getHoveredWidget(int mouseX, int mouseY);
    void arranged$updateWidget(AdvancementWidget widget);
    void arranged$updateIndex(int index);
}
