package net.styna.stynastmc.lightoverlay;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.compat.ModCompat;
import net.styna.stynastmc.config.ConfigKey;
import org.joml.Matrix4f;

/** Light level overlay ported from MiniHUD by masa, fixed to MiniHUD's default settings. */
public final class LightOverlay {
    public static final KeyMapping KEY = new KeyMapping("key.stynastmc.light_overlay",
            ModCompat.isLoaded(ModCompat.MORE_OVERLAYS) ? InputConstants.UNKNOWN.getValue() : InputConstants.KEY_F7, ConfigKey.CATEGORY);

    private static final Identifier NUMBERS = StynaSTMC.id("textures/misc/light_level_numbers.png");
    private static final int RANGE = 24;
    private static final int SAFE = 1;
    private static final int DIM = 0;
    private static final float MARKER_LOW = (1.0F - 0.84F) / 2.0F;
    private static final float MARKER_HIGH = 1.0F - MARKER_LOW;
    private static final float NUMBER_OFFSET_X = 0.26F;
    private static final float NUMBER_OFFSET_Z = 0.32F;
    private static final float LIFT = 0.01F;
    private static final float LINE_WIDTH = 1.6F;
    private static final int NUMBER_LIT = 0xFF20FF40;
    private static final int NUMBER_DIM = 0xFFC0C040;
    private static final int NUMBER_DARK = 0xFFC03030;
    private static final int MARKER_LIT = 0xFF209040;
    private static final int MARKER_DIM = 0xFFC0C040;
    private static final int MARKER_SKY_LIT = 0xFFFFFF33;
    private static final int MARKER_DARK = 0xFFFF4848;

    private record Spot(int x, int y, int z, int block, int sky) {
    }

    private static boolean enabled;
    private static List<Spot> spots = List.of();
    private static BlockPos lastScan;
    private static int ticksSinceScan;

    private LightOverlay() {
    }

    public static void toggle() {
        enabled = !enabled;
        spots = List.of();
        lastScan = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable(enabled ? "stynastmc.light_overlay.on" : "stynastmc.light_overlay.off"));
        }
    }

    public static void tick(Minecraft minecraft) {
        while (KEY.consumeClick()) {
            toggle();
        }
        if (!enabled || minecraft.level == null || minecraft.player == null) {
            return;
        }
        BlockPos center = minecraft.player.blockPosition();
        // MiniHUD rescans after moving 4 blocks; the timer also catches torches placed or broken.
        if (lastScan == null || ++ticksSinceScan >= 10 || center.distManhattan(lastScan) >= 4) {
            spots = scan(minecraft.level, center);
            lastScan = center;
            ticksSinceScan = 0;
        }
    }

    public static void submit(SubmitNodeCollector collector, CameraRenderState camera) {
        List<Spot> current = spots;
        Minecraft minecraft = Minecraft.getInstance();
        if (!enabled || current.isEmpty() || minecraft.player == null) {
            return;
        }
        Vec3 cam = camera.pos;
        Direction facing = minecraft.player.getDirection();
        PoseStack poseStack = new PoseStack();
        collector.submitCustomGeometry(poseStack, RenderTypes.text(NUMBERS), (pose, buffer) -> {
            float ox = NUMBER_OFFSET_X;
            float oz = NUMBER_OFFSET_Z;
            float nx = switch (facing) {
                case SOUTH -> ox;
                case WEST -> -oz;
                case EAST -> oz;
                default -> -ox;
            };
            float nz = switch (facing) {
                case SOUTH -> oz;
                case WEST -> ox;
                case EAST -> -ox;
                default -> -oz;
            };
            Matrix4f m = pose.pose();
            for (Spot spot : current) {
                int color = spot.block < SAFE ? NUMBER_DARK : spot.block > DIM ? NUMBER_LIT : NUMBER_DIM;
                number(buffer, m, (float) (spot.x - nx - cam.x), (float) (spot.y + LIFT - cam.y),
                        (float) (spot.z - nz - cam.z), facing, spot.block, color);
            }
        });
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            for (Spot spot : current) {
                if (spot.block >= SAFE) {
                    continue;
                }
                int color = spot.sky >= SAFE ? MARKER_SKY_LIT : MARKER_DARK;
                float x = (float) (spot.x - cam.x);
                float y = (float) (spot.y + LIFT - cam.y);
                float z = (float) (spot.z - cam.z);
                line(buffer, pose, x + MARKER_LOW, y, z + MARKER_LOW, x + MARKER_LOW, y, z + MARKER_HIGH, color);
                line(buffer, pose, x + MARKER_LOW, y, z + MARKER_HIGH, x + MARKER_HIGH, y, z + MARKER_HIGH, color);
                line(buffer, pose, x + MARKER_HIGH, y, z + MARKER_HIGH, x + MARKER_HIGH, y, z + MARKER_LOW, color);
                line(buffer, pose, x + MARKER_HIGH, y, z + MARKER_LOW, x + MARKER_LOW, y, z + MARKER_LOW, color);
            }
        });
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        buffer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, dx / len, 0.0F, dz / len).setLineWidth(LINE_WIDTH);
        buffer.addVertex(pose, x2, y2, z2).setColor(color).setNormal(pose, dx / len, 0.0F, dz / len).setLineWidth(LINE_WIDTH);
    }

    // The texture is a 4x4 grid of the digits 0-15; corner order turns the number to face the player.
    private static void number(VertexConsumer buffer, Matrix4f m, float x, float y, float z, Direction facing, int level, int color) {
        float w = 0.25F;
        float u = (level & 3) * w;
        float v = (level >> 2) * w;
        switch (facing) {
            case SOUTH -> {
                vertex(buffer, m, x + 1, y, z + 1, u, v, color);
                vertex(buffer, m, x + 1, y, z, u, v + w, color);
                vertex(buffer, m, x, y, z, u + w, v + w, color);
                vertex(buffer, m, x, y, z + 1, u + w, v, color);
            }
            case WEST -> {
                vertex(buffer, m, x, y, z + 1, u, v, color);
                vertex(buffer, m, x + 1, y, z + 1, u, v + w, color);
                vertex(buffer, m, x + 1, y, z, u + w, v + w, color);
                vertex(buffer, m, x, y, z, u + w, v, color);
            }
            case EAST -> {
                vertex(buffer, m, x + 1, y, z, u, v, color);
                vertex(buffer, m, x, y, z, u, v + w, color);
                vertex(buffer, m, x, y, z + 1, u + w, v + w, color);
                vertex(buffer, m, x + 1, y, z + 1, u + w, v, color);
            }
            default -> {
                vertex(buffer, m, x, y, z, u, v, color);
                vertex(buffer, m, x, y, z + 1, u, v + w, color);
                vertex(buffer, m, x + 1, y, z + 1, u + w, v + w, color);
                vertex(buffer, m, x + 1, y, z, u + w, v, color);
            }
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f m, float x, float y, float z, float u, float v, int color) {
        buffer.addVertex(m, x, y, z).setColor(color).setUv(u, v).setLight(LightCoordsUtil.FULL_BRIGHT);
    }

    private static List<Spot> scan(Level level, BlockPos center) {
        List<Spot> found = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = center.getX() - RANGE;
        int maxX = center.getX() + RANGE;
        int minZ = center.getZ() - RANGE;
        int maxZ = center.getZ() + RANGE;
        int startY = Math.max(center.getY() - RANGE, level.getMinY());
        int endY = Math.min(center.getY() + RANGE, level.getMaxY() + 1);
        int topY = level.getMaxY() + 1;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int y = startY; y <= endY; y++) {
                    if (y > startY && chunk.getSection(chunk.getSectionIndex(y - 1)).hasOnlyAir()) {
                        continue;
                    }
                    for (int x = Math.max(cx << 4, minX); x <= Math.min((cx << 4) + 15, maxX); x++) {
                        for (int z = Math.max(cz << 4, minZ); z <= Math.min((cz << 4) + 15, maxZ); z++) {
                            if (!canSpawnAt(level, chunk, pos, x, y, z)) {
                                continue;
                            }
                            pos.set(x, y, z);
                            int block = y < topY ? level.getBrightness(LightLayer.BLOCK, pos) : 0;
                            int sky = y < topY ? level.getBrightness(LightLayer.SKY, pos) : 15;
                            found.add(new Spot(x, y, z, block, sky));
                        }
                    }
                }
            }
        }
        return found;
    }

    // Dry spots only, matching MiniHUD's default of skipping underwater spawns.
    private static boolean canSpawnAt(Level level, LevelChunk chunk, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        pos.set(x, y - 1, z);
        if (!chunk.getBlockState(pos).isValidSpawn(level, pos, EntityTypes.CREEPER)) {
            return false;
        }
        pos.set(x, y, z);
        BlockState state = chunk.getBlockState(pos);
        if (!NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, state.getFluidState(), EntityTypes.WITHER_SKELETON)) {
            return false;
        }
        pos.set(x, y + 1, z);
        return NaturalSpawner.isValidEmptySpawnBlock(level, pos, chunk.getBlockState(pos), state.getFluidState(), EntityTypes.WITHER_SKELETON);
    }
}
