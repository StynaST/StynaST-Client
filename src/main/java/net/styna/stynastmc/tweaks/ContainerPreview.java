package net.styna.stynastmc.tweaks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.util.Text;

/** Container contents preview, ported from ShulkerBoxTooltip by MisterPeModder (MIT). */
public final class ContainerPreview {
    private static final ResourceLocation BACKGROUND = Ui.sprite("tooltip/container_preview");
    // Unknown formatting codes draw nothing, so this tags the blank lines that make room for the grid.
    private static final String MARKER = "§z§y";
    private static final int SLOT = 18;
    private static final int BORDER = 7;
    private static final int COLUMNS = 9;
    private static final int SHULKER_SLOTS = 27;
    private static final int LINE = 10;

    private ContainerPreview() {
    }

    private static boolean enabled() {
        return ContainerPreviewConfig.on(ContainerPreviewConfig.ENABLED);
    }

    private static boolean requested() {
        return ContainerPreviewConfig.on(ContainerPreviewConfig.ALWAYS_ON) || GuiScreen.isShiftKeyDown();
    }

    private static boolean full() {
        return GuiScreen.isShiftKeyDown() && GuiScreen.isAltKeyDown();
    }

    private static NBTTagCompound blockEntityTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("BlockEntityTag", 10) ? tag.getCompoundTag("BlockEntityTag") : null;
    }

    private static boolean previewable(ItemStack stack) {
        if (!enabled()) {
            return false;
        }
        NBTTagCompound tag = blockEntityTag(stack);
        if (tag == null || tag.hasKey("LootTable", 8) || !tag.hasKey("Items", 9)) {
            return false;
        }
        NBTTagList items = tag.getTagList("Items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            if (!new ItemStack(items.getCompoundTagAt(i)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** True while the preview grid replaces vanilla's item list. */
    public static boolean showing(ItemStack stack) {
        return requested() && previewable(stack);
    }

    public static void onTooltip(ItemStack stack, List<String> lines) {
        if (!previewable(stack)) {
            return;
        }
        if (showing(stack)) {
            Grid grid = grid(stack);
            int rows = (grid.windowHeight() + 4 + LINE - 1) / LINE;
            // Spaces are 4px wide, so this line is as wide as the grid.
            lines.add(MARKER + Text.repeat(" ", (grid.windowWidth() + 3) / 4));
            for (int i = 1; i < rows; i++) {
                lines.add(MARKER);
            }
        }
        String key;
        String hint;
        if (!requested()) {
            key = "Shift";
            hint = "stynastmc.tooltip.preview.compact";
        } else if (!full()) {
            key = ContainerPreviewConfig.on(ContainerPreviewConfig.ALWAYS_ON) ? "Alt+Shift" : "Alt";
            hint = "stynastmc.tooltip.preview.full";
        } else {
            return;
        }
        lines.add("§6" + key + ": §f" + Text.tr(hint));
    }

    public static void render(ItemStack stack, List<String> lines, int x, int y) {
        if (!showing(stack)) {
            return;
        }
        int first = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(MARKER)) {
                first = i;
                break;
            }
        }
        if (first < 0) {
            return;
        }
        Grid grid = grid(stack);
        int top = y + first * LINE + (first > 0 ? 2 : 0);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 400);
        Ui.sprite(BACKGROUND, x, top, grid.windowWidth(), grid.windowHeight(), grid.color);
        GlStateManager.popMatrix();

        Minecraft mc = Minecraft.getMinecraft();
        RenderItem render = mc.getRenderItem();
        FontRenderer font = mc.fontRenderer;
        float oldZ = render.zLevel;
        render.zLevel = 400;
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        for (int i = 0; i < grid.items.size(); i++) {
            ItemStack item = grid.items.get(i);
            if (item.isEmpty()) {
                continue;
            }
            int sx = x + BORDER + 1 + (i % COLUMNS) * SLOT;
            int sy = top + BORDER + 1 + (i / COLUMNS) * SLOT;
            render.renderItemAndEffectIntoGUI(item, sx, sy);
            render.renderItemOverlayIntoGUI(font, item, sx, sy, item.getCount() == 1 ? "" : count(item.getCount()));
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        render.zLevel = oldZ;
    }

    private static Grid grid(ItemStack stack) {
        List<ItemStack> slots = slots(stack);
        boolean compact = !full();
        return new Grid(compact ? merge(slots) : slots, color(stack));
    }

    private static List<ItemStack> slots(ItemStack stack) {
        NBTTagCompound tag = blockEntityTag(stack);
        NBTTagList list = tag.getTagList("Items", 10);
        int last = -1;
        for (int i = 0; i < list.tagCount(); i++) {
            last = Math.max(last, list.getCompoundTagAt(i).getByte("Slot") & 0xFF);
        }
        int size = isShulker(stack) ? SHULKER_SLOTS : Math.max(COLUMNS, (last / COLUMNS + 1) * COLUMNS);
        NonNullList<ItemStack> slots = NonNullList.withSize(Math.max(size, last + 1), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, slots);
        return slots;
    }

    // Same item, damage and NBT stack together, biggest totals first.
    private static List<ItemStack> merge(List<ItemStack> slots) {
        Map<String, ItemStack> merged = new LinkedHashMap<>();
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                continue;
            }
            String key = stack.getItem().getRegistryName() + "@" + stack.getMetadata() + "#" + stack.getTagCompound();
            ItemStack existing = merged.get(key);
            if (existing == null) {
                merged.put(key, stack.copy());
            } else {
                existing.grow(stack.getCount());
            }
        }
        List<ItemStack> list = new ArrayList<>(merged.values());
        list.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        return list;
    }

    private static boolean isShulker(ItemStack stack) {
        return Block.getBlockFromItem(stack.getItem()) instanceof BlockShulkerBox;
    }

    private static int color(ItemStack stack) {
        if (!isShulker(stack)) {
            return 0xFFFFFFFF;
        }
        EnumDyeColor dye = BlockShulkerBox.getColorFromItem(stack.getItem());
        // Dark dyes would make the window unreadable, so each channel keeps a floor.
        int rgb = dye.getColorValue();
        int r = Math.max(38, (rgb >> 16) & 0xFF);
        int g = Math.max(38, (rgb >> 8) & 0xFF);
        int b = Math.max(38, rgb & 0xFF);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static String count(int count) {
        if (count < 1000) {
            return Integer.toString(count);
        }
        return count < 1_000_000 ? count / 1000 + "k" : count / 1_000_000 + "M";
    }

    private static final class Grid {
        final List<ItemStack> items;
        final int color;
        final int columns;
        final int rows;

        Grid(List<ItemStack> items, int color) {
            this.items = items;
            this.color = color;
            int size = Math.max(1, items.size());
            columns = Math.min(COLUMNS, size);
            rows = (size + COLUMNS - 1) / COLUMNS;
        }

        int windowWidth() {
            return BORDER * 2 + columns * SLOT;
        }

        int windowHeight() {
            return BORDER * 2 + rows * SLOT;
        }
    }
}
