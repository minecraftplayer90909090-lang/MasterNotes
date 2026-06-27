package com.abhaythmaster.masternotes;

import com.abhaythmaster.masternotes.goal.GoalManager;
import com.abhaythmaster.masternotes.gui.MasterDashboardScreen;
import com.abhaythmaster.masternotes.keybind.MasterNotesKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class MasterNotesClient implements ClientModInitializer {

    public static final String MOD_ID = "masternotes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        MasterNotesKeys.register();
        GoalManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (MasterNotesKeys.OPEN_KEY.wasPressed()) {
                client.setScreen(new MasterDashboardScreen());
            }
        });

        LOGGER.info("[MasterNotes] Loaded! By AbhayTheMaster. Press M to open.");
    }
}
