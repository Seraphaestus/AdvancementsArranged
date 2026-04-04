package amaryllis.advancements_arranged.mixin;

import amaryllis.advancements_arranged.PersistentData;
import amaryllis.advancements_arranged.Util;
import amaryllis.advancements_arranged.mixin_interfaces.IAdvancementTab;
import amaryllis.advancements_arranged.mixin_interfaces.IAdvancementWidget;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mixin(AdvancementWidget.class)
public class AdvancementWidgetMixin implements IAdvancementWidget {

    private static final int SIZE = 26;
    private static final int HALF_SIZE = SIZE / 2;
    private static final int SHADOW = -16777216;
    private static final int MAX_BEND_DISTANCE = 64;

    @Shadow private int x;
    @Shadow private int y;

    @Shadow private AdvancementTab tab;
    @Shadow private AdvancementNode advancementNode;

    @Shadow @Nullable private AdvancementWidget parent;
    @Shadow private List<AdvancementWidget> children;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void loadPosition(CallbackInfo callback) {
        PersistentData.loadSavedPosition(advancementNode.holder(), (AdvancementWidget)(Object)this);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        if (tab != null) ((IAdvancementTab)tab).arranged$updateWidget((AdvancementWidget)(Object)this);
    }

    @Overwrite
    public void drawConnectivity(GuiGraphics guiGraphics, int scrollX, int scrollY, boolean dropShadow) {
        if (parent != null) {
            // +3px horizontal offset because the icon has invisible padding to the left for some reason
            int startX = parent.getX() + HALF_SIZE + scrollX + 3;
            int startY = parent.getY() + HALF_SIZE + scrollY;

            int endX = x + HALF_SIZE + scrollX + 3;
            int endY = y + HALF_SIZE + scrollY;

            int deltaX = endX - startX;
            int deltaY = endY - startY;

            boolean verticalAnchors = deltaX > deltaY;
            if (deltaX < SIZE) verticalAnchors = true;
            else if (deltaY < SIZE) verticalAnchors = false;

            int endAnchorX =  verticalAnchors ? endX : endX - Math.min(deltaX / 2, MAX_BEND_DISTANCE);
            int endAnchorY = !verticalAnchors ? endY : endY - Math.min(deltaY / 2, MAX_BEND_DISTANCE);

            int startAnchorX =  verticalAnchors ? startX : endAnchorX;
            int startAnchorY = !verticalAnchors ? startY : endAnchorY;

            int thickness = dropShadow ? 1 : 0;
            int color = dropShadow ? SHADOW : -1;

            Util.line(guiGraphics, startX, startY, startAnchorX, startAnchorY, thickness, color);
            Util.line(guiGraphics, startAnchorX, startAnchorY, endAnchorX, endAnchorY, thickness, color);
            Util.line(guiGraphics, endAnchorX, endAnchorY, endX, endY, thickness, color);
        }

        for (AdvancementWidget child: children) {
            child.drawConnectivity(guiGraphics, scrollX, scrollY, dropShadow);
        }
    }

}
