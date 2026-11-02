package net.styna.stynastmc.pickups;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.styna.stynastmc.ui.Ui;

/**
 * Item and experience pickup feed. Each row is a dark band docked to the screen edge with an arrow shaped inner end,
 * holding a lighter rarity tinted tab for the icon and carried total. Layout is measured from the docked edge so
 * both sides mirror.
 */
public final class PickupNotifications {
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_GAP = 2;
    private static final int PAD = 5;
    private static final int SPACE = 4;
    private static final int TAB_HEIGHT = 12;
    private static final int TAB_LEAD = 7;
    // Drawn past the screen edge so the overshoot of the slide in never shows a gap.
    private static final int BLEED = 24;
    private static final long ENTER_MS = 800;
    private static final long EXIT_MS = 1000;
    private static final long PULSE_MS = 500;
    private static final int BAND = 0x68000000;
    private static final int TAB_BODY = 0x30FFFFFF;
    private static final int COMMON_RIM = 0x90FFFFFF;
    private static final int TOTAL_COLOR = 0xFFDDDDDD;
    private static final int EPIC_COLOR = 0xFFC46BFF;
    private static final int XP_COLOR = 0xFFC5E862;
    private static final ItemStack XP_ICON = new ItemStack(Items.EXPERIENCE_BOTTLE);
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static long lastFrame;

    private PickupNotifications() {
    }

    public static void onPickup(Entity from, int amount) {
        if (!PickupConfig.on(PickupConfig.ENABLED)) {
            return;
        }
        if (from instanceof ItemEntity item && PickupConfig.ITEMS.get()) {
            ItemStack stack = item.getItem();
            if (!stack.isEmpty() && amount > 0) {
                add(stack, amount);
            }
        } else if (from instanceof ExperienceOrb orb && PickupConfig.XP.get() && orb.getValue() > 0) {
            add(ItemStack.EMPTY, orb.getValue());
        }
    }

    public static void clear() {
        ENTRIES.clear();
    }

    private static void add(ItemStack stack, int amount) {
        long now = Util.getMillis();
        for (Entry entry : ENTRIES) {
            if (!entry.leaving(now) && entry.matches(stack)) {
                entry.count += amount;
                entry.refreshed = now;
                entry.pulse = now;
                return;
            }
        }
        ENTRIES.add(0, new Entry(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1), amount, now));
        // Push the oldest still showing out early so the list stays within its cap.
        int showing = 0;
        for (Entry entry : ENTRIES) {
            if (!entry.leaving(now) && ++showing > PickupConfig.MAX_ENTRIES.get()) {
                entry.refreshed = now - displayMs();
            }
        }
    }

    private static long displayMs() {
        return (long) (PickupConfig.DISPLAY_TIME.get() * 1000);
    }

    public static void render(GuiGraphicsExtractor g, boolean hidden) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min((now - lastFrame) / 1000F, 0.25F);
        lastFrame = now;
        ENTRIES.removeIf(entry -> now > entry.refreshed + displayMs() + EXIT_MS);
        if (ENTRIES.isEmpty() || hidden || minecraft.player == null || !PickupConfig.on(PickupConfig.ENABLED)) {
            return;
        }
        PickupConfig.Corner corner = PickupConfig.CORNER.get();
        float follow = 1 - (float) Math.exp(-dt * 12);
        for (int slot = 0; slot < ENTRIES.size(); slot++) {
            Entry entry = ENTRIES.get(slot);
            float target = PickupConfig.Y_OFFSET.get() + slot * (ROW_HEIGHT + ROW_GAP);
            entry.offset = entry.placed ? Mth.lerp(follow, entry.offset, target) : target;
            entry.placed = true;
            int y = corner.bottom() ? Math.round(g.guiHeight() - entry.offset - ROW_HEIGHT) : Math.round(entry.offset);
            new Row(g, minecraft.font, minecraft.player, entry, now, corner.right(), y).draw();
        }
    }

    /** One notification, positioned by distance from the docked screen edge. */
    private static final class Row {
        final GuiGraphicsExtractor g;
        final Font font;
        final Entry entry;
        final boolean right;
        final int y;
        final int edge;
        final float alpha;
        final float pulse;
        final long now;
        final Component count;
        final Component name;
        final Component total;

        Row(GuiGraphicsExtractor g, Font font, Player player, Entry entry, long now, boolean right, int y) {
            this.g = g;
            this.font = font;
            this.entry = entry;
            this.right = right;
            this.y = y;
            this.now = now;
            float enter = Mth.clamp((now - entry.born) / (float) ENTER_MS, 0, 1);
            float exit = Mth.clamp((now - entry.refreshed - displayMs()) / (float) EXIT_MS, 0, 1);
            alpha = Math.min(Mth.clamp(enter * 2, 0, 1), 1 - exit);
            pulse = entry.pulse == 0 ? 0 : pulse(Mth.clamp((now - entry.pulse) / (float) PULSE_MS, 0, 1));
            count = Component.literal("x" + abbreviate(entry.count));
            name = name(font, entry);
            total = PickupConfig.SHOW_TOTAL.get() ? total(player, entry) : null;
            float slide = (1 - easeOutBack(enter)) + easeInCubic(exit);
            int shift = Math.round(slide * (width() + 8));
            int offset = PickupConfig.X_OFFSET.get();
            edge = right ? g.guiWidth() - offset + shift : offset - shift;
        }

        int totalWidth() {
            return total == null ? 0 : font.width(total) + SPACE;
        }

        int iconStart() {
            return PAD + totalWidth();
        }

        int width() {
            return iconStart() + 16 + TAB_LEAD + font.width(name) + SPACE + font.width(count) + PAD;
        }

        void draw() {
            if (alpha <= 0.02F) {
                return;
            }
            band(width());
            tab(iconStart() + 16 + TAB_LEAD - 3);
            if (entry.glows()) {
                glow(iconStart() + 8);
            }
            int textY = y + (ROW_HEIGHT - font.lineHeight) / 2 + 1;
            int d = PAD;
            if (total != null) {
                text(total, d, textY, TOTAL_COLOR);
                d += totalWidth();
            }
            icon(d);
            d += 16 + TAB_LEAD;
            text(name, d, textY, Ui.WHITE);
            d += font.width(name) + SPACE;
            text(count, d, textY, Ui.MUTED);
        }

        /** Screen x of a span that starts {@code from} and ends {@code to} pixels in from the edge. */
        int x0(int from, int to) {
            return right ? edge - to : edge + from;
        }

        void span(int from, int to, int top, int height, int color) {
            int x = x0(from, to);
            g.fill(x, top, x + (to - from), top + height, fade(color, alpha));
        }

        // Rows step in pairs toward the middle, giving a blunt arrow tip.
        static int arrow(int row, int height) {
            return Math.abs(2 * row - (height - 1)) / 4;
        }

        void band(int width) {
            for (int r = 0; r < ROW_HEIGHT; r++) {
                span(-BLEED, width - arrow(r, ROW_HEIGHT), y + r, 1, BAND);
            }
        }

        /** Lighter tab under the icon and total, rimmed in the rarity color with its own arrow tip. */
        void tab(int length) {
            int top = y + (ROW_HEIGHT - TAB_HEIGHT) / 2;
            int rim = accent(entry);
            int depth = arrow(0, TAB_HEIGHT);
            for (int r = 0; r < TAB_HEIGHT; r++) {
                int end = length + depth - arrow(r, TAB_HEIGHT);
                boolean edgeRow = r == 0 || r == TAB_HEIGHT - 1;
                span(-BLEED, end - 1, top + r, 1, edgeRow ? rim : TAB_BODY);
                span(end - 1, end, top + r, 1, rim);
            }
            // Second, fainter chevron just inside the tip.
            for (int r = 2; r < TAB_HEIGHT - 2; r++) {
                int end = length + depth - arrow(r, TAB_HEIGHT) - 3;
                span(end - 1, end, top + r, 1, rim & 0x00FFFFFF | 0x50000000);
            }
        }

        /** Soft turning glow behind the icon, for epic items and experience. */
        void glow(int center) {
            int cx = right ? edge - center : edge + center;
            int cy = y + ROW_HEIGHT / 2;
            int color = entry.xp() ? XP_COLOR : EPIC_COLOR;
            float spin = (now % 8_000) / 8_000F * Mth.TWO_PI;
            float breathe = 0.85F + 0.15F * Mth.sin(now / 400F);
            g.pose().pushMatrix();
            g.pose().translate(cx, cy);
            for (int layer = 0; layer < 3; layer++) {
                int size = Math.round((12 - layer * 3) * breathe);
                int a = 0x28 + layer * 0x18;
                g.pose().pushMatrix();
                g.pose().rotate(spin * (layer % 2 == 0 ? 1 : -1) + layer * 0.4F);
                g.fill(-size, -size, size, size, fade(color & 0x00FFFFFF | a << 24, alpha));
                g.pose().popMatrix();
            }
            g.pose().popMatrix();
        }

        void icon(int from) {
            int x = x0(from, from + 16);
            float scale = 1 + 0.25F * pulse;
            g.pose().pushMatrix();
            g.pose().translate(x + 8, y + ROW_HEIGHT / 2F);
            g.pose().scale(scale, scale);
            g.item(entry.icon(), -8, -8);
            g.pose().popMatrix();
        }

        void text(Component text, int from, int textY, int color) {
            g.text(font, text, x0(from, from + font.width(text)), textY, fade(color, alpha), true);
        }
    }

    private static Component name(Font font, Entry entry) {
        Component name = entry.xp() ? Component.translatable("stynastmc.pickups.experience").withColor(XP_COLOR & 0xFFFFFF)
                : entry.stack.getStyledHoverName();
        int max = PickupConfig.MAX_NAME_WIDTH.get();
        if (font.width(name) <= max) {
            return name;
        }
        FormattedText cut = font.substrByWidth(name, max - font.width("..."));
        return Component.literal(cut.getString() + "...").withStyle(name.getStyle());
    }

    private static Component total(Player player, Entry entry) {
        if (entry.xp()) {
            return Component.translatable("stynastmc.pickups.level", player.experienceLevel);
        }
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, entry.stack)) {
                total += stack.getCount();
            }
        }
        return total > 0 ? Component.literal(abbreviate(total)) : null;
    }

    private static int accent(Entry entry) {
        if (entry.xp()) {
            return XP_COLOR;
        }
        return switch (entry.stack.getRarity()) {
            case UNCOMMON -> Ui.GROUP_LABEL;
            case RARE -> Ui.FOCUS_OUTLINE;
            case EPIC -> EPIC_COLOR;
            default -> COMMON_RIM;
        };
    }

    private static String abbreviate(int value) {
        if (value < 10_000) {
            return Integer.toString(value);
        }
        double scaled = value < 1_000_000 ? value / 1_000.0 : value / 1_000_000.0;
        String text = String.format(Locale.ROOT, "%.1f", scaled);
        return (text.endsWith(".0") ? text.substring(0, text.length() - 2) : text) + (value < 1_000_000 ? "k" : "M");
    }

    private static int fade(int color, float alpha) {
        return ARGB.color(Math.round(ARGB.alpha(color) * alpha), color);
    }

    // Quick swell over the first tenth, then an elastic settle.
    private static float pulse(float t) {
        if (t < 0.1F) {
            return easeOutCubic(t / 0.1F);
        }
        float u = (t - 0.1F) / 0.9F;
        return 1 - easeOutElastic(u);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        return 1 + (c1 + 1) * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    private static float easeOutElastic(float t) {
        if (t <= 0 || t >= 1) {
            return t <= 0 ? 0 : 1;
        }
        return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1);
    }

    private static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static final class Entry {
        final ItemStack stack;
        final long born;
        int count;
        long refreshed;
        long pulse;
        float offset;
        boolean placed;

        Entry(ItemStack stack, int count, long now) {
            this.stack = stack;
            this.count = count;
            this.born = now;
            this.refreshed = now;
        }

        boolean xp() {
            return stack.isEmpty();
        }

        boolean glows() {
            return xp() || stack.getRarity() == Rarity.EPIC;
        }

        ItemStack icon() {
            return xp() ? XP_ICON : stack;
        }

        boolean matches(ItemStack other) {
            return other.isEmpty() ? xp() : !xp() && ItemStack.isSameItemSameComponents(stack, other);
        }

        boolean leaving(long now) {
            return now > refreshed + PickupNotifications.displayMs();
        }
    }
}
