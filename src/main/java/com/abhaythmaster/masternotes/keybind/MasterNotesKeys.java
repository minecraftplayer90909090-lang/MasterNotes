package com.abhaythmaster.masternotes.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MasterNotesKeys {

    public static KeyBinding OPEN_KEY;

    public static void register() {
        OPEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.masternotes.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.masternotes"
        ));
    }
}
