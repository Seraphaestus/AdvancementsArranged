package amaryllis.advancements_arranged.mixin;

import amaryllis.advancements_arranged.Config;
import amaryllis.advancements_arranged.PersistentData;
import amaryllis.advancements_arranged.mixin_interfaces.IAdvancementTab;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

import static net.minecraft.client.gui.screens.advancements.AdvancementTabType.MAX_TABS;
import static net.minecraft.client.gui.screens.advancements.AdvancementsScreen.*;

@OnlyIn(Dist.CLIENT)
@Mixin(AdvancementTab.class)
public class AdvancementTabMixin implements IAdvancementTab {

    @Shadow private int page;
    @Shadow private int index;

    @Shadow private AdvancementsScreen screen;
    @Shadow private Map<AdvancementHolder, AdvancementWidget> widgets;
    @Shadow private AdvancementWidget root;

    @Shadow private int minX;
    @Shadow private int minY;
    @Shadow private int maxX;
    @Shadow private int maxY;

    @Shadow private double scrollX;
    @Shadow private double scrollY;

    @Shadow private DisplayInfo display;
    @Shadow private float fade;
    @Shadow private boolean centered;

    public AdvancementWidget arranged$getHoveredWidget(int mouseX, int mouseY) {
        int iScrollX = Mth.floor(scrollX);
        int iScrollY = Mth.floor(scrollY);

        for (Map.Entry<AdvancementHolder, AdvancementWidget> advancement: widgets.entrySet()) {
            AdvancementWidget widget = advancement.getValue();
            if (widget.isMouseOver(iScrollX, iScrollY, mouseX, mouseY)) return widget;
        }
        return null;
    }

    public void arranged$updateWidget(AdvancementWidget widget) {
        minX = Math.min(minX, widget.getX());
        maxX = Math.max(maxX, widget.getX() + 28);
        minY = Math.min(minY, widget.getY());
        maxY = Math.max(maxY, widget.getY() + 27);
    }

    public void arranged$updateIndex(int index) {
        this.index = index % MAX_TABS;
        this.page = index / MAX_TABS;
    }

    public Map<AdvancementHolder, AdvancementWidget> getWidgets() {
        return widgets;
    }

    @Overwrite
    public void scroll(double dragX, double dragY) {
        scrollX += dragX;
        scrollY += dragY;
    }

    //region Modify Window Size
    @Overwrite
    public void drawContents(GuiGraphics guiGraphics, int x, int y) {
        int width = Config.WINDOW_WIDTH.getAsInt() - WINDOW_INSIDE_X * 2;
        int height = Config.WINDOW_HEIGHT.getAsInt() - WINDOW_INSIDE_Y - WINDOW_INSIDE_X; // Top border > bottom border

        if (!centered) {
            scrollX = (double)((width - 1 - maxX - minX) / 2);
            scrollY = (double)((height - 1 - maxY - minY) / 2);
            centered = true;
        }

        guiGraphics.enableScissor(x, y, x + width, y + height);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);

        // Draw tiling background
        ResourceLocation background = display.getBackground().orElse(TextureManager.INTENTIONAL_MISSING_TEXTURE);
        int iScrollX = Mth.floor(scrollX);
        int iScrollY = Mth.floor(scrollY);
        int bgX = iScrollX % 16;
        int bgY = iScrollY % 16;
        int tilesAcross = Mth.ceil(width / 16f);
        int tilesDown = Mth.ceil(height / 16f);
        for (int tileX = -1; tileX <= tilesAcross; ++tileX) {
            for (int tileY = -1; tileY <= tilesDown; ++tileY) {
                int tilePosX = bgX + 16 * tileX;
                int tilePosY = bgY + 16 * tileY;
                ResourceLocation texture = PersistentData.selectTexture((AdvancementTab)(Object)this, tilePosX - iScrollX, tilePosY - iScrollY, background);
                guiGraphics.blit(texture, tilePosX, tilePosY, 0, 0, 16, 16, 16, 16);
            }
        }

        // Overlay
        final double backgroundDarkness = Config.WINDOW_BACKGROUND_DARKNESS.getAsDouble();
        if (backgroundDarkness > 0) guiGraphics.fill(0, 0, width, height, Mth.floor(backgroundDarkness * 255) << 24);

        root.drawConnectivity(guiGraphics, iScrollX, iScrollY, true);
        root.drawConnectivity(guiGraphics, iScrollX, iScrollY, false);
        root.draw(guiGraphics, iScrollX, iScrollY);

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }
    @Overwrite
    public void drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int width, int height) {
        guiGraphics.pose().pushPose();

        final int windowWidth = Config.WINDOW_WIDTH.getAsInt() - WINDOW_INSIDE_X * 2;
        final int windowHeight = Config.WINDOW_HEIGHT.getAsInt() - WINDOW_INSIDE_Y - WINDOW_INSIDE_X;

        guiGraphics.pose().translate(0, 0, -200);
        guiGraphics.fill(0, 0, windowWidth, windowHeight, Mth.floor(fade * 255) << 24);

        boolean isInBounds = mouseX > 0 && mouseX < windowWidth && mouseY > 0 && mouseY < windowHeight;
        boolean isHoveringAdvancement = false;
        int x = Mth.floor(scrollX);
        int y = Mth.floor(scrollY);
        if (isInBounds) {
            for (AdvancementWidget widget: widgets.values()) {
                if (widget.isMouseOver(x, y, mouseX, mouseY)) {
                    isHoveringAdvancement = true;
                    widget.drawHover(guiGraphics, x, y, fade, width, height);
                    break;
                }
            }
        }
        guiGraphics.pose().popPose();

        fade = isHoveringAdvancement
                ? Mth.clamp(fade + 0.02F, 0.0F, 0.3F)
                : Mth.clamp(fade - 0.04F, 0.0F, 1.0F);
    }
}
