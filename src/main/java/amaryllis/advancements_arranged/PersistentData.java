package amaryllis.advancements_arranged;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import amaryllis.advancements_arranged.mixin_interfaces.IAdvancementTab;
import amaryllis.advancements_arranged.mixin_interfaces.IAdvancementWidget;
import com.google.gson.*;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Tuple;
import org.joml.Vector2i;

import static amaryllis.advancements_arranged.AdvancementsArranged.LOGGER;

// Based on code from EMI

public class PersistentData {
    public static final File FILE = new File(AdvancementsArranged.MODID + ".json");
    public static final Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();

    private static final Map<String, Vector2i> advancementPositions = new HashMap<>();
    private static final Map<String, List<Tuple<String, Integer>>> backgroundTextures = new HashMap<>();
    private static final Random random = new Random();

    public static void save(Map<AdvancementHolder, AdvancementTab> advancements, AdvancementTab selectedTab) {
        try {
            JsonObject previousContents = GSON.fromJson(new FileReader(FILE), JsonObject.class);
            JsonObject json = new JsonObject();

            JsonObject positions = previousContents.has("positions") ? previousContents.getAsJsonObject("positions") : new JsonObject();
            for (AdvancementTab tab: advancements.values()) {
                var tabAdvancements = ((IAdvancementTab)tab).getWidgets().entrySet();
                for (Map.Entry<AdvancementHolder, AdvancementWidget> advancement: tabAdvancements) {
                    String advancementID = advancement.getKey().toString();
                    AdvancementWidget widget = advancement.getValue();

                    Util.putJsonArray(positions, advancementID, widget.getX(), widget.getY());
                }
            }
            json.add("positions", positions);

            if (previousContents.has("backgrounds")) json.add("backgrounds", previousContents.get("backgrounds"));

            FileWriter writer = new FileWriter(FILE);
            GSON.toJson(json, writer);
            writer.close();

        } catch (Exception e) {
            LOGGER.error("Failed to write persistent data", e);
        }
    }

    public static void load(AdvancementsScreen advancementsScreen) {
        if (!FILE.exists()) return;

        try {
            JsonObject json = GSON.fromJson(new FileReader(FILE), JsonObject.class);

            advancementPositions.clear();
            if (GsonHelper.isObjectNode(json, "positions")) {
                JsonObject positions = json.getAsJsonObject("positions");
                for (String advancementID: positions.keySet()) {
                    Vector2i pos = Util.getJsonArrayAsVector2i(positions, advancementID);
                    if (pos != null) advancementPositions.put(advancementID, pos);
                }
            }

            backgroundTextures.clear();
            if (GsonHelper.isObjectNode(json, "backgrounds")) {
                JsonObject backgrounds = json.getAsJsonObject("backgrounds");
                for (String tabID: backgrounds.keySet()) {
                    var pool = Util.getJsonAsPool(backgrounds, tabID);
                    if (pool != null) backgroundTextures.put(tabID, pool);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to parse persistent data", e);
        }
    }

    public static void loadSavedPosition(AdvancementHolder advancement, AdvancementWidget widget) {
        final String advancementID = advancement.toString();
        if (advancementPositions.containsKey(advancementID)) {
            final Vector2i pos = advancementPositions.get(advancementID);
            ((IAdvancementWidget)widget).setPosition(pos.x, pos.y);
        }
    }

    public static ResourceLocation selectTexture(AdvancementTab tab, int tileX, int tileY, ResourceLocation fallback) {
        final String tabID = Util.getAdvancementTabID(tab);
        if (!backgroundTextures.containsKey(tabID)) return fallback;

        var pool = backgroundTextures.get(tabID);
        if (pool.isEmpty()) return fallback;

        int cumulativeWeight = 0;
        for (Tuple<String, Integer> poolEntry: pool) cumulativeWeight += poolEntry.getB();

        random.setSeed(tileX + 1664525L * tileY);
        int randomWeight = random.nextInt(cumulativeWeight);

        cumulativeWeight = 0;
        for (Tuple<String, Integer> poolEntry: pool) {
            cumulativeWeight += poolEntry.getB();
            if (randomWeight < cumulativeWeight) {
                return ResourceLocation.parse(poolEntry.getA());
            }
        }

        return fallback;
    }
}