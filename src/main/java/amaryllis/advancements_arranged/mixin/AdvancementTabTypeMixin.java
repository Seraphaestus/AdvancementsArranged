package amaryllis.advancements_arranged.mixin;

import amaryllis.advancements_arranged.Config;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementTabType.class)
public class AdvancementTabTypeMixin {

    // Only allows ABOVE type tabs

    @Shadow int width;
    @Shadow int height;
    @Shadow int max;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setMax(CallbackInfo cbi) {
        int ordinal = ((Enum)(Object)this).ordinal();
        if (ordinal != 0) max = 0;
        else max = Mth.floor((Config.WINDOW_WIDTH.getAsInt() - 4) / 31f);
    }

    @Overwrite
    public int getX(int index) {
        return (width + 4) * index;
    }

    @Overwrite
    public int getY(int index) {
        return -height + 4;
    }

}