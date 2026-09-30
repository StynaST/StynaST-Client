package net.styna.stynastmc.tweaks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.styna.stynastmc.StynaSTMC;

/** Container contents preview, ported from ShulkerBoxTooltip by MisterPeModder (MIT). */
public final class ContainerPreview {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(StynaSTMC.MODID, "tooltip/container_preview");
    private static final int SLOT = 18;
    private static final int BORDER = 7;
    private static final int COLUMNS = 9;
    private static final int SHULKER_SLOTS = 27;
    private static final int UNDYED_SHULKER = 0x976797;

    public record Data(List<ItemStack> items, boolean compact, int color) implements TooltipComponent {
    }

    private ContainerPreview() {
    }

    private static boolean enabled() {
        return ContainerPreviewConfig.on(ContainerPreviewConfig.ENABLED);
    }

    private static boolean requested() {
        return ContainerPreviewConfig.on(ContainerPreviewConfig.ALWAYS_ON) || Minecraft.getInstance().hasShiftDown();
    }

    private static boolean full() {
        return Minecraft.getInstance().hasShiftDown() && Minecraft.getInstance().hasAltDown();
    }

    private static boolean previewable(ItemStack stack) {
        if (!enabled() || stack.has(DataComponents.CONTAINER_LOOT)) {
            return false;
        }
        TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (display.hideTooltip() || !display.shows(DataComponents.CONTAINER)) {
            return false;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents.nonEmptyItems().iterator().hasNext();
    }

    /** True while the preview grid replaces vanilla's item list. */
    public static boolean showing(ItemStack stack) {
        return requested() && previewable(stack);
    }

    public static Optional<TooltipComponent> image(ItemStack stack) {
        if (!showing(stack)) {
            return Optional.empty();
        }
        List<ItemStack> slots = slots(stack);
        boolean compact = !full();
        return Optional.of(new Data(compact ? merge(slots) : slots, compact, color(stack)));
    }

    public static void addHint(ItemStack stack, List<Component> lines) {
        if (!previewable(stack)) {
            return;
        }
        Component key;
        String hint;
        if (!requested()) {
            key = Component.literal("Shift");
            hint = "stynastmc.tooltip.preview.compact";
        } else if (!full()) {
            key = Component.literal(ContainerPreviewConfig.on(ContainerPreviewConfig.ALWAYS_ON) ? "Alt+Shift" : "Alt");
            hint = "stynastmc.tooltip.preview.full";
        } else {
            return;
        }
        lines.add(Component.empty().append(key).append(": ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(hint).withStyle(ChatFormatting.WHITE)));
    }

    private static List<ItemStack> slots(ItemStack stack) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> all = contents.itemCopies().toList();
        int last = all.size() - 1;
        while (last >= 0 && all.get(last).isEmpty()) {
            last--;
        }
        int size = isShulker(stack) ? SHULKER_SLOTS : Math.max(COLUMNS, (last / COLUMNS + 1) * COLUMNS);
        NonNullList<ItemStack> slots = NonNullList.withSize(Math.max(size, last + 1), ItemStack.EMPTY);
        contents.copyInto(slots);
        return slots;
    }

    // Same item and components stack together, biggest totals first.
    private static List<ItemStack> merge(List<ItemStack> slots) {
        Map<Key, ItemStack> merged = new LinkedHashMap<>();
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                continue;
            }
            merged.merge(new Key(stack), stack.copy(), (a, b) -> {
                a.grow(b.getCount());
                return a;
            });
        }
        List<ItemStack> list = new ArrayList<>(merged.values());
        list.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
        return list;
    }

    private record Key(ItemStack stack) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && ItemStack.isSameItemSameComponents(stack, key.stack);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stack.getItem(), stack.getComponents());
        }
    }

    private static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock;
    }

    private static int color(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock shulker)) {
            return 0xFFFFFFFF;
        }
        DyeColor dye = shulker.getColor();
        if (dye == null) {
            return 0xFF000000 | UNDYED_SHULKER;
        }
        // Dark dyes would make the window unreadable, so each channel keeps a floor.
        int rgb = dye.getTextureDiffuseColor();
        int r = Math.max(38, (rgb >> 16) & 0xFF);
        int g = Math.max(38, (rgb >> 8) & 0xFF);
        int b = Math.max(38, rgb & 0xFF);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static final class Client implements ClientTooltipComponent {
        private final Data data;
        private final int columns;
        private final int rows;

        public Client(Data data) {
            this.data = data;
            int size = Math.max(1, data.items().size());
            columns = Math.min(COLUMNS, size);
            rows = (size + COLUMNS - 1) / COLUMNS;
        }

        private int windowWidth() {
            return BORDER * 2 + columns * SLOT;
        }

        private int windowHeight() {
            return BORDER * 2 + rows * SLOT;
        }

        @Override
        public int getHeight(Font font) {
            return windowHeight() + 4;
        }

        @Override
        public int getWidth(Font font) {
            return windowWidth();
        }

        @Override
        public void extractImage(Font font, int x, int y, int totalWidth, int totalHeight, GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, windowWidth(), windowHeight(), data.color());
            List<ItemStack> items = data.items();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                int sx = x + BORDER + 1 + (i % COLUMNS) * SLOT;
                int sy = y + BORDER + 1 + (i / COLUMNS) * SLOT;
                graphics.item(stack, sx, sy);
                graphics.itemDecorations(font, stack, sx, sy, stack.getCount() == 1 ? "" : count(stack.getCount()));
            }
        }

        private static String count(int count) {
            if (count < 1000) {
                return Integer.toString(count);
            }
            return count < 1_000_000 ? count / 1000 + "k" : count / 1_000_000 + "M";
        }
    }
}
