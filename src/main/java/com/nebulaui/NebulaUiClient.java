package com.nebulaui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.glfw.GLFW;

public class NebulaUiClient implements ClientModInitializer {
    private boolean wasDown = false;

    @Override
    public void onInitializeClient() {
        // Poll Right Shift directly so it works in-game, on the title screen and on the pause menu.
        ClientTickEvents.START_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register(HudOverlay::render);
        System.out.println("[Nebula UI] Loaded. Press Right Shift to open the menu.");
    }

    private void tick(MinecraftClient client) {
        if (client.getWindow() == null) return;
        boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (!pressed) return;

        Screen cur = client.currentScreen;
        if (cur instanceof NebulaScreen) {
            cur.close();
        } else if (cur == null || cur instanceof TitleScreen || cur instanceof GameMenuScreen) {
            client.setScreen(new NebulaScreen(cur));
        }
    }
}
