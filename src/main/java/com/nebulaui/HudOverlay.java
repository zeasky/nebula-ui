package com.nebulaui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Small in-game HUD driven by the menu toggles. */
public final class HudOverlay {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private HudOverlay() {}

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        int y = 6;
        if (Settings.fps || Settings.hud) y = line(ctx, mc, "FPS " + mc.getCurrentFps(), y);
        if (Settings.coords || Settings.hud) {
            y = line(ctx, mc, String.format("XYZ %.1f / %.1f / %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()), y);
        }
        if (Settings.clock || Settings.hud) y = line(ctx, mc, LocalTime.now().format(FMT), y);
    }

    private static int line(DrawContext ctx, MinecraftClient mc, String s, int y) {
        Text bt = Text.literal(s).formatted(Formatting.BOLD);
        int w = mc.textRenderer.getWidth(bt);
        ctx.fill(4, y - 2, 4 + w + 6, y + 10, 0x9A0C0818);
        ctx.fill(4, y - 2, 6, y + 10, 0xFF9B3DF0);
        ctx.drawText(mc.textRenderer, bt, 9, y, 0xFFE9D5FF, true);
        return y + 14;
    }
}
