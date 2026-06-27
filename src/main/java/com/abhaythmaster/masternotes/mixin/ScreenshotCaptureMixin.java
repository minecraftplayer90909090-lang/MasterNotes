package com.abhaythmaster.masternotes.mixin;

import com.abhaythmaster.masternotes.screenshot.ScreenshotMetaManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(ScreenshotRecorder.class)
public class ScreenshotCaptureMixin {

    /**
     * Injected AFTER a screenshot is saved.
     * We look for the newest PNG in the screenshots directory and write a
     * .meta.json file beside it that stores server IP / world name / timestamp.
     */
    @Inject(method = "saveScreenshot*", at = @At("RETURN"))
    private static void masternotes_afterScreenshot(CallbackInfo ci) {
        try {
            File screenshotsDir = new File(MinecraftClient.getInstance().runDirectory, "screenshots");
            ScreenshotMetaManager.captureContext(screenshotsDir);
        } catch (Exception e) {
            // Never crash the game for metadata
        }
    }
}
