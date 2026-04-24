package amaryllis.advancements_arranged.mixin;

import amaryllis.advancements_arranged.Config;
import amaryllis.advancements_arranged.PersistentData;
import amaryllis.advancements_arranged.Util;
import amaryllis.advancements_arranged.mixin_interfaces.IAdvancementTab;
import amaryllis.advancements_arranged.mixin_interfaces.IAdvancementWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

import static net.minecraft.client.gui.screens.advancements.AdvancementsScreen.*;

@OnlyIn(Dist.CLIENT)
@Mixin(AdvancementsScreen.class)
public class AdvancementsScreenMixin extends Screen implements ClientAdvancements.Listener {

    private static final int LEFT_CLICK = 0;
    private static final int DRAG_SNAP = 4;
    private static final int BACKGROUND_TEXTURE_WIDTH = 252;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 140;

    @Shadow private Map<AdvancementHolder, AdvancementTab> tabs;
    @Shadow @Nullable private AdvancementTab selectedTab;
    @Shadow private static int tabPage;
    @Shadow private static int maxPages;

    @Shadow private ClientAdvancements advancements;
    private AdvancementWidget draggedAdvancement;
    private int dragOffsetX;
    private int dragOffsetY;

    @Shadow private boolean isScrolling;

    protected AdvancementsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void reloadSavedArrangement(CallbackInfo callback) {
        PersistentData.load((AdvancementsScreen)(Object)this);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    public void onClose(CallbackInfo callback) {
        if (Config.CAN_EDIT.isTrue() && Config.TEST_ONLY.isFalse())
            PersistentData.save(tabs, selectedTab);
    }

    @Inject(method = "onAddAdvancementRoot", at = @At("TAIL"))
    private void resortTabs(CallbackInfo callback) {
        var ordered_tabs = tabs.values().stream().sorted(Config::AdvancementTabSorter).toList();
        for (int i = 0; i < ordered_tabs.size(); i++) {
            ((IAdvancementTab)ordered_tabs.get(i)).arranged$updateIndex(i);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        draggedAdvancement = null;

        if (button == LEFT_CLICK) {
            // Handle switching tabs
            int offsetX = (width - Config.WINDOW_WIDTH.getAsInt()) / 2;
            int offsetY = (height - Config.WINDOW_HEIGHT.getAsInt()) / 2;

            for (AdvancementTab tab: this.tabs.values()) {
                if (tab.getPage() == tabPage && tab.isMouseOver(offsetX, offsetY, mouseX, mouseY)) {
                    advancements.setSelectedTab(tab.getRootNode().holder(), true);
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }

            if (selectedTab == null || Config.CAN_EDIT.isFalse())
                return super.mouseClicked(mouseX, mouseY, button);

            // Handle advancement rearranging
            int widgetMouseX = (int)mouseX - offsetX - WINDOW_INSIDE_X;
            int widgetMouseY = (int)mouseY - offsetY - WINDOW_INSIDE_Y;
            var selectedWidget = ((IAdvancementTab) selectedTab).arranged$getHoveredWidget(widgetMouseX, widgetMouseY);

            if (selectedWidget != null) {
                draggedAdvancement = selectedWidget;
                dragOffsetX = selectedWidget.getX() - (int)mouseX;
                dragOffsetY = selectedWidget.getY() - (int)mouseY;
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) {
            isScrolling = false;
            return false;
        }

        if (draggedAdvancement != null) {
            int newPosX = (int)mouseX + dragOffsetX;
            int newPosY = (int)mouseY + dragOffsetY;
            if (Screen.hasShiftDown()) {
                newPosX = DRAG_SNAP * (newPosX / DRAG_SNAP);
                newPosY = DRAG_SNAP * (newPosY / DRAG_SNAP);
            }
            ((IAdvancementWidget)draggedAdvancement).setPosition(newPosX, newPosY);

            if (isScrolling) isScrolling = false;

        } else {
            if (!isScrolling) isScrolling = true;
            else if (selectedTab != null) selectedTab.scroll(dragX, dragY);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggedAdvancement != null) draggedAdvancement = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Inject(method = "renderTooltips", at = @At("HEAD"), cancellable = true)
    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int offsetX, int offsetY, CallbackInfo callback) {
        if (draggedAdvancement != null) callback.cancel();
    }

    @ModifyVariable(method = "init", at = @At(value = "STORE"), ordinal = 0)
    private int modifyWidth_init(int original) {
        return (width - Config.WINDOW_WIDTH.getAsInt()) / 2;
    }
    @ModifyVariable(method = "init", at = @At(value = "STORE"), ordinal = 1)
    private int modifyHeight_init(int original) {
        return (height - Config.WINDOW_HEIGHT.getAsInt()) / 2;
    }
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button$Builder;pos(II)Lnet/minecraft/client/gui/components/Button$Builder;", ordinal = 1))
    private Button.Builder modifyWidth_init_2(Button.Builder builder, int x, int y) {
        return builder.pos(x - 252 + Config.WINDOW_WIDTH.getAsInt(), y);
    }

    @Overwrite
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int windowWidth = Config.WINDOW_WIDTH.getAsInt();
        int windowHeight = Config.WINDOW_HEIGHT.getAsInt();
        int x = (width - windowWidth) / 2;
        int y = (height - windowHeight) / 2;
        if (maxPages != 0) {
            Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
            int fontWidth = font.width(page);
            guiGraphics.drawString(font, page.getVisualOrderText(), x + (windowWidth - fontWidth) / 2, y - 38, -1);
        }

        renderInside(guiGraphics, mouseX, mouseY, x, y);
        renderWindow(guiGraphics, x, y);
        renderTooltips(guiGraphics, mouseX, mouseY, x, y);
    }
    @Overwrite
    public void renderWindow(GuiGraphics guiGraphics, int offsetX, int offsetY) {
        RenderSystem.enableBlend();
        Util.blitNinePatch(guiGraphics, AdvancementsScreen.WINDOW_LOCATION, offsetX, offsetY,
                BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT,
                Config.WINDOW_WIDTH.getAsInt(), Config.WINDOW_HEIGHT.getAsInt(),
                Config.WINDOW_BACKGROUND_MARGIN_X.getAsInt(), Config.WINDOW_BACKGROUND_MARGIN_Y.getAsInt());

        if (tabs.size() > 1) {
            for (AdvancementTab tab: tabs.values()) {
                if (tab.getPage() == tabPage) tab.drawTab(guiGraphics, offsetX, offsetY, tab == selectedTab);
            }
            for (AdvancementTab tab: tabs.values()) {
                if (tab.getPage() == tabPage) tab.drawIcon(guiGraphics, offsetX, offsetY);
            }
        }

        guiGraphics.drawString(font, selectedTab != null ? selectedTab.getTitle() : TITLE, offsetX + WINDOW_TITLE_X, offsetY + WINDOW_TITLE_Y, 4210752, false);
    }
    @Overwrite
    private void renderInside(GuiGraphics guiGraphics, int mouseX, int mouseY, int offsetX, int offsetY) {
        offsetX += WINDOW_INSIDE_X;
        offsetY += WINDOW_INSIDE_Y;
        if (selectedTab != null) {
            selectedTab.drawContents(guiGraphics, offsetX, offsetY);
            return;
        }

        int width = Config.WINDOW_WIDTH.getAsInt() - WINDOW_INSIDE_X * 2;
        int height = Config.WINDOW_HEIGHT.getAsInt() - WINDOW_INSIDE_Y - WINDOW_INSIDE_X;

        guiGraphics.fill(offsetX, offsetY, offsetX + width, offsetY + height, -16777216);
        guiGraphics.drawCenteredString(font, NO_ADVANCEMENTS_LABEL, offsetX + width / 2, offsetY + height / 2 - 4, -1);
        guiGraphics.drawCenteredString(font, VERY_SAD_LABEL,        offsetX + width / 2, offsetY + height - WINDOW_INSIDE_Y, -1);
    }

    @Shadow public void onAddAdvancementRoot(AdvancementNode node) {}
    @Shadow public void onRemoveAdvancementRoot(AdvancementNode node) {}
    @Shadow public void onAddAdvancementTask(AdvancementNode node) {}
    @Shadow public void onRemoveAdvancementTask(AdvancementNode node) {}
    @Shadow public void onAdvancementsCleared() {}
    @Shadow public void onUpdateAdvancementProgress(AdvancementNode node, AdvancementProgress progress) {}
    @Shadow public void onSelectedTabChanged(@Nullable AdvancementHolder advancementHolder) {}

    @Shadow private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int offsetX, int offsetY) {}
}
