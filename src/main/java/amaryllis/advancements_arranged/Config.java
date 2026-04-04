package amaryllis.advancements_arranged;

import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue CAN_EDIT;
    public static final ModConfigSpec.IntValue WINDOW_WIDTH;
    public static final ModConfigSpec.IntValue WINDOW_HEIGHT;
    public static final ModConfigSpec.IntValue WINDOW_BACKGROUND_MARGIN_X;
    public static final ModConfigSpec.IntValue WINDOW_BACKGROUND_MARGIN_Y;
    public static final ModConfigSpec.DoubleValue WINDOW_BACKGROUND_DARKNESS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TAB_SORT_ORDER;

    static {
        CAN_EDIT = BUILDER
                .define("can_edit", true);

        WINDOW_WIDTH = BUILDER
                .comment("")
                .comment("It is recommended that additional size is added in increments of 16 so the background can tile nicely")
                .comment("Note: the vanilla size is (252, 140)")
                .defineInRange("window_width", 380, 0, 1024);
        WINDOW_HEIGHT = BUILDER
                .defineInRange("window_height", 204, 0, 1024);

        WINDOW_BACKGROUND_MARGIN_X = BUILDER
                .comment("")
                .comment("Margins for splitting the advancement screen background into a nine-patch rect")
                .comment("The background can be located at minecraft:textures/gui/advancements/window")
                .defineInRange("window_background_margin_x", 32, 0, 1024);
        WINDOW_BACKGROUND_MARGIN_Y = BUILDER
                .defineInRange("window_background_margin_y", 32, 0, 1024);

        WINDOW_BACKGROUND_DARKNESS = BUILDER
                .comment("")
                .defineInRange("window_background_darkness", 0.25, 0d, 1d);

        TAB_SORT_ORDER = BUILDER
                .comment("")
                .comment("Ordered list of root advancement IDs which dictates the order their corresponding tabs should be displayed")
                .comment("Non-listed tabs will be displayed after the end of the listed tabs in arbitrary order")
                .defineListAllowEmpty("tab_sort_order",
                        List.of("minecraft:story", "minecraft:husbandry", "minecraft:adventure", "minecraft:nether", "minecraft:end"),
                        (id) -> true);

        SPEC = BUILDER.build();
    }

    public static int AdvancementTabSorter(AdvancementTab a, AdvancementTab b) {
        final int a_ordinal = TAB_SORT_ORDER.get().indexOf(Util.getAdvancementTabID(a));
        final int b_ordinal = TAB_SORT_ORDER.get().indexOf(Util.getAdvancementTabID(b));
        return (a_ordinal != b_ordinal)
                ? a_ordinal - b_ordinal
                : a.getIndex() - b.getIndex();
    }
}