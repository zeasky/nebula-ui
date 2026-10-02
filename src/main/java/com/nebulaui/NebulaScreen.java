package com.nebulaui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

public class NebulaScreen extends Screen {
    private static final int HH = 18;       // header height
    private static final int PAD = 4;       // bottom padding of an open panel
    private static final int GAP = 5;
    private static final int TOP = 26;
    private static final float R = 6f;      // panel corner radius

    private int ih = 14;                    // row height (auto-fitted so lists don't need scrolling)

    private static final String[] COMBAT = {
            "Aim Assist", "Anchor Macro", "Auto Crystal", "Auto Double Hand", "Auto Hit Crystal",
            "Auto Inv Totem", "Auto Jump Reset", "Auto Totem", "Crystal Optimizer", "Double Anchor",
            "Elytra Swap", "HitBox", "Hover Totem", "Mace Bomber", "Mace Swap", "No Hit Delay",
            "Shield Breaker", "Spear Swap", "Static HitBoxes", "Totem Offhand", "Trigger Bot"
    };
    private static final String[] MISC = {
            "Auto Clicker", "Auto Eat", "Auto Firework", "Auto Log", "Auto Loot", "Auto Mine",
            "Auto Reconnect", "Auto Tool", "Auto Tpa", "Auto Walk", "Cord Snapper", "Elytra Glide",
            "FakePlayer", "Fast Place", "Freecam", "Key Pearl", "Key Wind Charge", "Name Protect",
            "Skin Protect", "Sprint", "Weather Notifier"
    };
    private static final String[] DONUT = {
            "Anti Trap", "Auto Sell", "Auto Spawner Sell", "Chunk Finder", "Fake Pay", "Fake Stats",
            "Item Dropper", "Netherite Finder", "Player Chunks", "Spawner Protect"
    };
    private static final String[] RENDER = {
            "Block ESP", "Block Notifier", "Free Look", "Fullbright", "HUD", "Jump Circles", "Mob ESP",
            "Name Tags", "Ore Sim", "Pearl Trajectory", "Player ESP", "RealHitbox", "Music HUD",
            "Storage ESP", "SwingSpeed", "Target HUD"
    };
    private static final String[] BASE = {
            "Block Entity Debug", "Hole ESP", "Light Finder", "Prime Chunk Finder", "Rtp Base Finder",
            "Sus Chunk Finder", "Suspicious ESP", "Tunnel Base Finder", "Seed Chunk Finder"
    };

    private final Screen parent;
    private final List<Panel> panels = new ArrayList<>();
    private List<List<Panel>> columns = new ArrayList<>();
    private final List<List<List<Panel>>> layouts = new ArrayList<>();   // preferred layouts, best first
    private boolean boldItems = true;
    private long openTime = 0, last = 0;

    private static final int N = 48;
    private final float[] px = new float[N], pv = new float[N], ps = new float[N], pp = new float[N];

    public NebulaScreen(Screen parent) {
        super(Text.literal("Nebula"));
        this.parent = parent;
        Random r = new Random(7);
        for (int i = 0; i < N; i++) {
            px[i] = r.nextFloat(); pv[i] = 6 + r.nextFloat() * 18;
            ps[i] = r.nextFloat(); pp[i] = r.nextFloat() * 6.28f;
        }

        Panel combat = new Panel("COMBAT");  addSwitches(combat, COMBAT);
        Panel misc = new Panel("MISC");      addSwitches(misc, MISC);
        Panel donut = new Panel("DONUT");    addSwitches(donut, DONUT);
        Panel base = new Panel("BASE FINDING"); addSwitches(base, BASE);

        Panel render = new Panel("RENDER");  addSwitches(render, RENDER);

        panels.add(combat); panels.add(misc); panels.add(donut); panels.add(base); panels.add(render);
        // 1) all five side by side; 2) fallback for narrow windows: Donut + Base Finding share a column
        layouts.add(List.of(List.of(combat), List.of(misc), List.of(donut), List.of(base), List.of(render)));
        layouts.add(List.of(List.of(combat), List.of(misc), List.of(donut, base), List.of(render)));
        for (Panel p : panels) p.open = true;
    }

    private void addSwitches(Panel p, String[] names) {
        for (String n : names) {
            if (p.title.equals("RENDER") && n.equals("HUD")) {   // this one is real: FPS + coords + clock
                p.add(n, () -> Settings.hud, () -> Settings.hud = !Settings.hud);
                continue;
            }
            String key = p.title + ":" + n;
            p.add(n, () -> Settings.on.contains(key), () -> { if (!Settings.on.remove(key)) Settings.on.add(key); });
        }
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    // ---------------------------------------------------------------- render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        if (openTime == 0) { openTime = now; last = now; }
        float dt = Math.min((now - last) / 1000f, 0.1f);
        last = now;
        float t = (now - openTime) / 1000f;
        float fade = ease(clamp01(t / 0.4f));
        int accent = accent(t);

        if (Settings.dim) ctx.fill(0, 0, width, height, argb((int) (175 * fade), 0x07040F));
        if (Settings.scanlines)
            for (int y = 0; y < height; y += 3) ctx.fill(0, y, width, y + 1, argb((int) (26 * fade), 0x000000));
        if (Settings.particles) drawParticles(ctx, t, fade, accent);

        String title = "N E B U L A";
        Text titleT = bold(title);
        int tw = textRenderer.getWidth(titleT);
        int tx = (width - tw) / 2;
        int ty = 9 - (int) ((1 - fade) * 14);
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.2);
        for (int o = 3; o >= 1; o--)
            for (int[] d : new int[][]{{o, 0}, {-o, 0}, {0, o}, {0, -o}})
                ctx.drawText(textRenderer, titleT, tx + d[0], ty + d[1],
                        argb((int) ((26 + 22 * pulse) * fade / o) + 6, accent), false);
        ctx.drawText(textRenderer, titleT, tx, ty, argb(Math.max(8, (int) (255 * fade)), 0xF3E8FF), false);

        layout();

        for (int i = 0; i < panels.size(); i++) {
            Panel p = panels.get(i);
            float local = ease(clamp01((t - 0.10f - i * 0.10f) / 0.5f));
            p.x = p.baseX;
            p.y = p.baseY - (int) ((1 - local) * 70);
            p.alpha = local;

            float target = p.open ? 1f : 0f;
            p.expand += (target - p.expand) * Math.min(1f, dt * 10f);
            if (Math.abs(target - p.expand) < 0.002f) p.expand = target;

            p.scroll += (p.scrollTarget - p.scroll) * Math.min(1f, dt * 14f);

            p.draw(ctx, mx, my, dt, t, accent);
        }

    }

    /** Picks the best layout that fits the window, then fits row height so nothing needs scrolling. */
    private void layout() {
        int avail = height - TOP - 18;
        int maxW = width - 8;

        // try: 5 columns bold -> 5 columns regular -> 4 columns bold -> 4 columns regular
        List<List<Panel>> chosen = layouts.get(layouts.size() - 1);
        boolean chosenBold = false;
        search:
        for (List<List<Panel>> cand : layouts) {
            for (boolean b : new boolean[]{true, false}) {
                int tot = GAP * (cand.size() - 1);
                for (List<Panel> col : cand) {
                    int cw = 84;
                    for (Panel p : col) cw = Math.max(cw, p.fitWidth(b));
                    tot += cw;
                }
                if (tot <= maxW) { chosen = cand; chosenBold = b; break search; }
            }
        }
        columns = chosen;
        boldItems = chosenBold;

        for (List<Panel> col : columns) {
            int cw = 84;
            for (Panel p : col) cw = Math.max(cw, p.fitWidth(boldItems));
            for (Panel p : col) p.w = cw;
        }

        // largest row height (11..16) at which every column fits on screen
        float best = 16;
        for (List<Panel> col : columns) {
            int items = 0, fixed = GAP * (col.size() - 1);
            for (Panel p : col) { items += p.items.size(); fixed += HH + PAD; }
            best = Math.min(best, (avail - fixed) / (float) items);
        }
        ih = Math.max(11, (int) best);

        // only if it still can't fit (very large GUI scale) does a list get a scrollbar
        for (Panel p : panels)
            p.visRows = Math.min(p.items.size(), Math.max(3, (avail - HH - PAD) / ih));

        int totalW = GAP * (columns.size() - 1);
        for (List<Panel> col : columns) totalW += col.get(0).w;
        int sx = (width - totalW) / 2;
        for (List<Panel> col : columns) {
            int y = TOP;
            for (Panel p : col) { p.baseX = sx; p.baseY = y; y += p.fullH() + GAP; }
            sx += col.get(0).w + GAP;
        }
    }

    private void drawParticles(DrawContext ctx, float t, float fade, int accent) {
        for (int i = 0; i < N; i++) {
            float y = height - ((t * pv[i] + ps[i] * height) % height);
            float x = px[i] * width + (float) Math.sin(t * 0.7f + pp[i]) * 14f;
            float tw = 0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(t * 2f + pp[i]));
            int size = ps[i] > 0.75f ? 2 : 1;
            int a = (int) (120 * tw * fade);
            ctx.fill((int) x - 1, (int) y - 1, (int) x + size + 1, (int) y + size + 1, argb(a / 4 + 1, accent));
            ctx.fill((int) x, (int) y, (int) x + size, (int) y + size, argb(a + 5, accent));
        }
    }

    // ----------------------------------------------------------------- input
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double mx = click.x(), my = click.y();
            for (Panel p : panels) {
                if (p.alpha < 0.9f) continue;
                if (mx >= p.x && mx <= p.x + p.w) {
                    if (my >= p.y && my <= p.y + HH) { p.open = !p.open; return true; }
                    if (p.expand > 0.9f && my >= p.y + HH && my < p.y + HH + p.visRows * ih) {
                        for (int i = 0; i < p.items.size(); i++) {
                            int iy = p.y + HH + i * ih - (int) p.scroll;
                            if (my >= iy && my < iy + ih) { p.items.get(i).toggle.run(); return true; }
                        }
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        for (Panel p : panels) {
            if (p.expand > 0.9f && p.items.size() > p.visRows
                    && mx >= p.x && mx <= p.x + p.w && my >= p.y + HH && my <= p.y + HH + p.visRows * ih) {
                float max = (p.items.size() - p.visRows) * ih;
                p.scrollTarget = Math.max(0f, Math.min(max, p.scrollTarget - (float) vertical * ih));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return super.keyPressed(input);
    }

    // ----------------------------------------------------------------- types
    private final class Item {
        final String name; final BooleanSupplier get; final Runnable toggle;
        float prog = -1, hover = 0;           // prog: 0 = off, 1 = on (animated)
        Item(String n, BooleanSupplier g, Runnable t) { name = n; get = g; toggle = t; }
    }

    private final class Panel {
        final String title; int w = 110; final List<Item> items = new ArrayList<>();
        boolean open; float expand, alpha, headHover, scroll, scrollTarget;
        int x, y, baseX, baseY, visRows = 3;

        Panel(String t) { title = t; }
        void add(String n, BooleanSupplier g, Runnable t) { items.add(new Item(n, g, t)); }
        int fullH() { return HH + visRows * ih + PAD; }
        int fitWidth(boolean boldNames) {
            int need = 9 + labelWidth(title, true) + 24;
            for (Item it : items) need = Math.max(need, 8 + labelWidth(it.name, boldNames) + 4 + 16 + 5);
            return need;
        }

        void draw(DrawContext ctx, int mx, int my, float dt, float time, int accent) {
            int bodyH = (int) ((visRows * ih + PAD) * expand);
            int h = HH + bodyH;
            int a = Math.max(6, (int) (255 * alpha));

            // soft, rounded outer glow
            int layers = Settings.strongGlow ? 16 : 12;
            int base = Settings.strongGlow ? 42 : 30;
            float breathe = 0.78f + 0.22f * (float) Math.sin(time * 2f + x * 0.02f);
            for (int i = layers; i >= 1; i--) {
                float f = 1f - (i - 1f) / layers;
                int al = (int) (base * f * f * breathe * alpha) + 1;
                rrect(ctx, x - i, y - i, x + w + i, y + h + i, R + i, accent, al);
            }

            // glowing rounded border + body
            rrect(ctx, x, y, x + w, y + h, R, accent, (int) (215 * alpha));
            rrect(ctx, x + 1, y + 1, x + w - 1, y + h - 1, R - 1, 0x0C0818, (int) (252 * alpha));

            // header gradient
            boolean hh = mx >= x && mx <= x + w && my >= y && my <= y + HH && alpha > 0.9f;
            headHover += ((hh ? 1f : 0f) - headHover) * Math.min(1f, dt * 14f);
            boolean collapsed = bodyH < 2;
            int hy2 = collapsed ? y + HH - 1 : y + HH;
            rrectV(ctx, x + 1, y + 1, x + w - 1, hy2, R - 1, accent,
                    (int) ((120 + 70 * headHover) * alpha), (int) (28 * alpha), true, collapsed);
            if (!collapsed)
                ctx.fill(x + 7, y + HH - 1, x + w - 7, y + HH, argb((int) (120 * alpha * expand), accent));

            drawLabel(ctx, title, x + 9, y + 5, argb(a, 0xFFFFFF), true, true);
            chevron(ctx, x + w - 15, y + 7, expand, argb(a, 0xFFFFFF));

            if (bodyH > 2) {
                int listTop = y + HH, listBottom = y + HH + visRows * ih;
                ctx.enableScissor(x + 1, y + HH, x + w - 1, y + h - 1);
                for (int i = 0; i < items.size(); i++) {
                    Item it = items.get(i);
                    int iy = y + HH + i * ih - (int) scroll;
                    if (iy + ih < listTop || iy > listBottom) continue;
                    boolean on = it.get.getAsBoolean();
                    boolean hov = expand > 0.9f && alpha > 0.9f
                            && mx >= x && mx <= x + w && my >= Math.max(iy, listTop)
                            && my < Math.min(iy + ih, listBottom);
                    it.hover += ((hov ? 1f : 0f) - it.hover) * Math.min(1f, dt * 16f);

                    float goal = on ? 1f : 0f;
                    if (it.prog < 0) it.prog = goal;
                    float step = dt * 5.5f;
                    if (it.prog < goal) it.prog = Math.min(goal, it.prog + step);
                    else if (it.prog > goal) it.prog = Math.max(goal, it.prog - step);
                    float e = easeInOut(it.prog);

                    if (it.hover > 0.01f)
                        rrect(ctx, x + 4, iy, x + w - 4, iy + ih, 4f, accent, (int) (48 * it.hover));
                    if (e > 0.02f)
                        rrect(ctx, x + 3, iy + 1, x + 5, iy + ih - 1, 1f, accent, (int) (235 * e));

                    drawLabel(ctx, it.name, x + 8 + (int) (it.hover * 2), iy + (ih - 8) / 2,
                            lerpColor(0x8E84B0, 0xF3E8FF, e) | 0xFF000000, false, boldItems);

                    // smooth pill toggle
                    int tx = x + w - 21, ty = iy + (ih - 8) / 2;
                    if (e > 0.02f) rrect(ctx, tx - 2, ty - 2, tx + 18, ty + 10, 6f, accent, (int) (46 * e));
                    rrect(ctx, tx, ty, tx + 16, ty + 8, 4f, lerpColor(0x2B2347, accent & 0xFFFFFF, e), 255);
                    float kw = 4f + 2f * (float) Math.sin(Math.PI * it.prog);
                    float kx = tx + 2 + e * (12f - kw);
                    rrect(ctx, kx, ty + 2, kx + kw, ty + 6, 2f, lerpColor(0x9A92BD, 0xFFFFFF, e), 255);
                }
                ctx.disableScissor();

                // scrollbar for long lists
                if (items.size() > visRows) {
                    float maxScroll = (items.size() - visRows) * ih;
                    int trackH = visRows * ih;
                    int thumbH = Math.max(14, trackH * visRows / items.size());
                    int thumbY = listTop + (int) ((trackH - thumbH) * (scroll / maxScroll));
                    rrect(ctx, x + w - 6, thumbY + 1, x + w - 3, thumbY + thumbH - 1, 1.5f, accent,
                            (int) (150 * expand));
                }
            }
        }
    }

    // --------------------------------------------------- smooth shape helpers
    /** Horizontal span with anti-aliased (fractional) left/right edges. */
    private static void block(DrawContext c, int ya, int yb, float xl, float xr, int rgb, int a) {
        if (yb <= ya || xr <= xl || a <= 0) return;
        int il = (int) Math.ceil(xl), ir = (int) Math.floor(xr);
        if (il > ir) { c.fill(ir, ya, ir + 1, yb, argb((int) (a * (xr - xl)), rgb)); return; }
        if (ir > il) c.fill(il, ya, ir, yb, argb(a, rgb));
        float lp = il - xl, rp = xr - ir;
        if (lp > 0.02f) c.fill(il - 1, ya, il, yb, argb((int) (a * lp), rgb));
        if (rp > 0.02f) c.fill(ir, ya, ir + 1, yb, argb((int) (a * rp), rgb));
    }

    private static float insetFor(int row, float r) {
        float dy = r - (row + 0.5f);
        if (dy <= 0) return 0;
        return r - (float) Math.sqrt(Math.max(0f, r * r - dy * dy));
    }

    private static void rrect(DrawContext c, float x1, int y1, float x2, int y2, float r, int rgb, int a) {
        rrectV(c, x1, y1, x2, y2, r, rgb, a, a, true, true);
    }

    /** Rounded rectangle with optional vertical alpha gradient and per-side rounding. */
    private static void rrectV(DrawContext c, float x1, int y1, float x2, int y2, float r, int rgb,
                               int aTop, int aBot, boolean rt, boolean rb) {
        int h = y2 - y1;
        if (h <= 0) return;
        r = Math.min(r, h / 2f);
        int ri = (int) Math.ceil(r);
        int topEnd = rt ? y1 + ri : y1, botStart = rb ? y2 - ri : y2;
        if (aTop == aBot && topEnd <= botStart) {
            for (int y = y1; y < topEnd; y++) {
                float in = insetFor(y - y1, r);
                block(c, y, y + 1, x1 + in, x2 - in, rgb, aTop);
            }
            block(c, topEnd, botStart, x1, x2, rgb, aTop);
            for (int y = botStart; y < y2; y++) {
                float in = insetFor(y2 - 1 - y, r);
                block(c, y, y + 1, x1 + in, x2 - in, rgb, aTop);
            }
            return;
        }
        for (int y = y1; y < y2; y++) {
            float f = (y - y1 + 0.5f) / h;
            int a = (int) (aTop + (aBot - aTop) * f);
            float in = 0;
            if (rt && y - y1 < ri) in = insetFor(y - y1, r);
            else if (rb && y2 - 1 - y < ri) in = insetFor(y2 - 1 - y, r);
            block(c, y, y + 1, x1 + in, x2 - in, rgb, a);
        }
    }

    private static void chevron(DrawContext ctx, int cx, int cy, float expand, int color) {
        boolean up = expand > 0.5f;
        for (int r = 0; r < 4; r++) {
            int w = 7 - r * 2;
            int row = up ? (3 - r) : r;
            ctx.fill(cx + r, cy + row, cx + r + w, cy + row + 1, color);
        }
    }

    /** Dark glowing purple that gently breathes (or rainbow if enabled). */
    private static int accent(float t) {
        if (Settings.rainbow) return 0xFF000000 | java.awt.Color.HSBtoRGB((t * 0.12f) % 1f, 0.65f, 1f);
        float k = 0.5f + 0.5f * (float) Math.sin(t * 1.1f);
        return 0xFF000000 | lerpColor(0x8B2FE0, 0xB45CFF, k);
    }

    private static int lerpColor(int a, int b, float t) {
        t = clamp01(t);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int argb(int a, int rgb) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    /** Draws text with a tighter gap between words (bold or regular). */
    private void drawLabel(DrawContext ctx, String s, int x, int y, int color, boolean shadow, boolean bold) {
        String[] words = s.split(" ");
        int cx = x;
        for (int i = 0; i < words.length; i++) {
            Text t = bold ? bold(words[i]) : Text.literal(words[i]);
            ctx.drawText(textRenderer, t, cx, y, color, shadow);
            cx += textRenderer.getWidth(t) + 3;
        }
    }

    private int labelWidth(String s, boolean bold) {
        String[] words = s.split(" ");
        int w = 0;
        for (int i = 0; i < words.length; i++) {
            Text t = bold ? bold(words[i]) : Text.literal(words[i]);
            w += textRenderer.getWidth(t) + (i < words.length - 1 ? 3 : 0);
        }
        return w;
    }

    private static Text bold(String s) { return Text.literal(s).formatted(Formatting.BOLD); }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float ease(float x) { float i = 1f - x; return 1f - i * i * i; }
    private static float easeInOut(float p) {
        return p < 0.5f ? 4f * p * p * p : 1f - (float) Math.pow(-2f * p + 2f, 3) / 2f;
    }
}
