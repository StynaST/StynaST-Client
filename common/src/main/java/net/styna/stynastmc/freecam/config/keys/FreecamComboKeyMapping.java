package net.styna.stynastmc.freecam.config.keys;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;


class FreecamComboKeyMapping extends FreecamKeyMapping {

    private final Runnable action;
    private final HoldAction holdAction;
    private final long maxHoldTicks;

    private long sinceLastUse = 0;
    private long sinceLastPress = 0;
    private long queued = 0;

    FreecamComboKeyMapping(String translationKey, InputConstants.Type type, int code, Runnable action, HoldAction holdAction, long maxHoldTicks) {
        super(translationKey, type, code);
        this.action = action;
        this.holdAction = holdAction;
        this.maxHoldTicks = maxHoldTicks;
    }

    @Override
    public void tick() {
        sinceLastUse++;
        sinceLastPress++;

        if (isDown()) {
            if (holdAction.run()) {
                markUsed();
            }
        } else {
            while (dequeue()) {
                action.run();
            }
        }
    }

    @Override
    public void setDown(boolean down) {
        if (down != isDown()) {
            if (down) {
                keyDown();
            } else {
                keyUp();
            }
        }
        super.setDown(down);
    }

    private void keyDown() {
        sinceLastPress = 0;
    }

    private void keyUp() {
        if (sinceLastUse > sinceLastPress && sinceLastPress < maxHoldTicks) {
            queued++;
        }
        markUsed();
    }

    private void markUsed() {
        sinceLastUse = 0;
    }

    private boolean dequeue() {
        if (queued < 1) {
            return false;
        }
        queued--;
        return true;
    }
}
