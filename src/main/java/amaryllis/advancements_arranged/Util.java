package amaryllis.advancements_arranged;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class Util {

    private static final ResourceLocation arrowsSprite = ResourceLocation.fromNamespaceAndPath(AdvancementsArranged.MODID, "arrows");

    public static void blitNinePatch(GuiGraphics guiGraphics, ResourceLocation textureID,
                 int x, int y, int textureWidth, int textureHeight, int width, int height, int marginX, int marginY) {
        final int x1 = marginX;
        final int x2 = width - marginX;
        final int inWidth = width - marginX * 2;
        final int u2 = textureWidth - marginX;
        final int inTexWidth = textureWidth - marginX * 2;

        final int y1 = marginY;
        final int y2 = height - marginY;
        final int inHeight = height - marginY * 2;
        final int v2 = textureHeight - marginY;
        final int inTexHeight = textureHeight - marginY * 2;

        // Corners                PosX    PosY    UVs     UV Size
        guiGraphics.blit(textureID, x     , y     ,  0,  0, marginX, marginY); // Top-left
        guiGraphics.blit(textureID, x + x2, y     , u2,  0, marginX, marginY); // Top-right
        guiGraphics.blit(textureID, x     , y + y2,  0, v2, marginX, marginY); // Bottom-left
        guiGraphics.blit(textureID, x + x2, y + y2, u2, v2, marginX, marginY); // Bottom-right

        // Sides                          PosX    PosY    UVs     Size               UV Size
        blitTiled(guiGraphics, textureID, x + x1, y     , x1,  0, inWidth,  marginY, inTexWidth, marginY); // Top-middle
        blitTiled(guiGraphics, textureID, x + x1, y + y2, x1, v2, inWidth,  marginY, inTexWidth, marginY); // Bottom-middle
        blitTiled(guiGraphics, textureID, x     , y + y1,  0, y1, marginX, inHeight, marginX, inTexHeight); // Middle-left
        blitTiled(guiGraphics, textureID, x + x2, y + y1, u2, y1, marginX, inHeight, marginX, inTexHeight); // Middle-right

        // Middle                 PosX    PosY    UVs     Size               UV Size
        blitTiled(guiGraphics, textureID, x + x1, y + y1, x1, y1, inWidth, inHeight, inTexWidth, inTexHeight);
    }

    public static void blitTiled(GuiGraphics guiGraphics, ResourceLocation textureID,
                 int x, int y, int textureX, int textureY, int width, int height, int textureWidth, int textureHeight) {
        final int tilesAcross = Mth.ceil(width / (float)textureWidth);
        final int tilesDown = Mth.ceil(height / (float)textureHeight);
        final int remainderX = width - (tilesAcross - 1) * textureWidth;
        final int remainderY = height - (tilesDown - 1) * textureHeight;
        for (int ix = 0; ix < tilesAcross; ix++) {
            for (int iy = 0; iy < tilesDown; iy++) {
                int tileWidth = (ix == tilesAcross - 1) ? remainderX : textureWidth;
                int tileHeight = (iy == tilesDown - 1) ? remainderY : textureHeight;
                guiGraphics.blit(textureID,
                        x + ix * textureWidth, y + iy * textureHeight,
                        textureX, textureY,
                        tileWidth, tileHeight
                );
            }
        }
    }

    public static void line(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int thickness, int color) {
        if (x0 > x1) {
            int temp = x0;
            x0 = x1;
            x1 = temp;
        }
        if (y0 > y1) {
            int temp = y0;
            y0 = y1;
            y1 = temp;
        }
        guiGraphics.fill(RenderType.gui(), x0 - thickness, y0 - thickness, x1 + 1 + thickness, y1 + 1 + thickness, color);
    }
    public static void square(GuiGraphics guiGraphics, int x, int y, int radius, int color) {
        guiGraphics.fill(RenderType.gui(), x - radius, y - radius, x + 1 + radius, y + 1 + radius, color);
    }

    public static void drawArrow(GuiGraphics guiGraphics, int x, int y, int anchorX, int anchorY, boolean verticalAnchors, int edgeDistanceX, int edgeDistanceY) {
        int width = verticalAnchors ? 7 : 5;
        int height = verticalAnchors ? 5 : 7;
        int u = 0, v = 0;

        if (verticalAnchors) {
            boolean downwards = (y < anchorY);
            y = moveTowards(y, anchorY, edgeDistanceY);
            x -= 3;
            if (downwards) {
                u = 8;
            } else {
                y -= 5;
            }
        }
        else {
            boolean rightwards = (x > anchorX);
            x = moveTowards(x, anchorX, edgeDistanceX);
            y -= 3;
            v = 6;
            if (rightwards) {
                u = 6;
                x -= 5;
            }
        }
        guiGraphics.blitSprite(arrowsSprite, 16, 16, u, v, x, y, width, height);
    }
    public static int moveTowards(int a, int b, int distance) {
        return (b > a) ? Math.min(a + distance, b) : Math.max(a - distance, b);
    }


    public static void putJsonArray(JsonObject json, String key, int... elements) {
        JsonArray array = new JsonArray();
        for (int element: elements) array.add(element);
        json.add(key, array);
    }
    public static Vector2i getJsonArrayAsVector2i(JsonObject json, String key) {
        if (!GsonHelper.isArrayNode(json, key)) return null;
        JsonArray array = json.getAsJsonArray(key);
        if (array.size() < 2) return null;

        try {
            int x = array.get(0).getAsInt();
            int y = array.get(1).getAsInt();
            return new Vector2i(x, y);
        } catch (UnsupportedOperationException | NumberFormatException | IllegalStateException e) {
            return null;
        }
    }
    public static List<Tuple<String, Integer>> getJsonAsPool(JsonObject json, String key) {
        if (!GsonHelper.isArrayNode(json, key)) return null;
        JsonArray poolData = json.getAsJsonArray(key);

        List<Tuple<String, Integer>> output = new ArrayList<>();
        for (int i = 0; i < poolData.size(); i++) {
            if (!poolData.get(i).isJsonObject()) continue;
            JsonObject poolEntry = poolData.get(i).getAsJsonObject();
            if (!poolEntry.has("texture")) continue;
            try {
                String texture = poolEntry.get("texture").getAsString();
                int weight = poolEntry.has("weight") ? poolEntry.get("weight").getAsInt() : 1;
                output.add(new Tuple<>(texture, weight));
            } catch (UnsupportedOperationException | IllegalStateException | NumberFormatException ignored) {}
        }
        return output;
    }

    public static String getAdvancementTabID(AdvancementTab tab) {
        final String root = tab.getRootNode().holder().toString();
        final int idx = root.indexOf('/');
        return (idx > -1) ? root.substring(0, root.indexOf('/')) : root;
    }

}
