package net.styna.stynastmc.pickups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Catches pickups that skip vanilla's take packet, like backpack pickup upgrades, which empty the item entity on the
 * server instead. Items in pickup reach that vanish or shrink without a take packet count as picked up.
 */
public final class PickupTracker {
    private static final double WATCH_RANGE = 4;
    // Server and client positions drift apart by a little under latency.
    private static final double REACH_SLACK = 0.5;
    private static final int TAKEN_TICKS = 100;
    // Despawning items also vanish; a few ticks early to cover the server's head start.
    private static final int DESPAWN_AGE = 6000 - 40;
    // Teleports unload nearby items in the same tick, which would look like pickups.
    private static final double TELEPORT_SQR = 4 * 4;

    private static final Map<Integer, Seen> SEEN = new HashMap<>();
    private static final Map<Integer, Integer> TAKEN = new HashMap<>();
    private static ClientLevel level;
    private static Vec3 lastPos;
    private static int tick;

    private record Seen(ItemStack stack, boolean reachable, int age) {
    }

    private PickupTracker() {
    }

    /** Called for every take packet, so items vanilla already reported or another player took are skipped. */
    public static void taken(int entityId) {
        TAKEN.put(entityId, tick + TAKEN_TICKS);
    }

    public static void clear() {
        SEEN.clear();
        TAKEN.clear();
        level = null;
        lastPos = null;
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.isSpectator()
                || !PickupConfig.on(PickupConfig.ENABLED) || !PickupConfig.ITEMS.get() || !PickupConfig.OTHER_MODS.get()) {
            clear();
            return;
        }
        if (minecraft.level != level) {
            clear();
            level = minecraft.level;
        }
        tick++;
        TAKEN.values().removeIf(expiry -> expiry < tick);
        Vec3 pos = player.position();
        if (lastPos != null && pos.distanceToSqr(lastPos) > TELEPORT_SQR) {
            SEEN.clear();
        }
        lastPos = pos;

        List<ItemStack> lost = new ArrayList<>();
        List<ItemStack> gained = new ArrayList<>();
        for (Map.Entry<Integer, Seen> entry : SEEN.entrySet()) {
            Seen before = entry.getValue();
            // Items that drifted out of the watched box are still alive; only gone or shrunk ones count.
            Entity now = level.getEntity(entry.getKey());
            int count = now instanceof ItemEntity item && !item.isRemoved() ? item.getItem().getCount() : 0;
            int delta = count - before.stack.getCount();
            if (delta > 0) {
                // A merge moves items between entities; the growing one cancels the vanished one.
                gained.add(before.stack.copyWithCount(delta));
            } else if (delta < 0 && before.reachable && before.age < DESPAWN_AGE && !TAKEN.containsKey(entry.getKey())) {
                lost.add(before.stack.copyWithCount(-delta));
            }
        }
        for (ItemStack gain : gained) {
            for (ItemStack loss : lost) {
                if (!loss.isEmpty() && ItemStack.isSameItemSameComponents(loss, gain)) {
                    int cancel = Math.min(loss.getCount(), gain.getCount());
                    loss.shrink(cancel);
                    gain.shrink(cancel);
                }
            }
        }
        for (ItemStack loss : lost) {
            if (!loss.isEmpty()) {
                PickupNotifications.onItems(loss, loss.getCount());
            }
        }

        SEEN.clear();
        AABB reach = player.getBoundingBox().inflate(1 + REACH_SLACK, 0.5 + REACH_SLACK, 1 + REACH_SLACK);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(WATCH_RANGE))) {
            ItemStack stack = item.getItem();
            if (!item.isRemoved() && !stack.isEmpty()) {
                SEEN.put(item.getId(), new Seen(stack.copy(), item.getBoundingBox().intersects(reach), item.getAge()));
            }
        }
    }
}
