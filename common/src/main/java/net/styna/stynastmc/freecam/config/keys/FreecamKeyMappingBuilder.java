package net.styna.stynastmc.freecam.config.keys;

import com.mojang.blaze3d.platform.InputConstants;

public class FreecamKeyMappingBuilder {
    private final String translationKey;
    private InputConstants.Type type = InputConstants.Type.KEYSYM;
    private int keyCode = InputConstants.UNKNOWN.getValue();
    private Runnable action;
    private HoldAction holdAction;
    private long maxTicks = 10;

    private FreecamKeyMappingBuilder(String translationKey) {
        this.translationKey = translationKey;
    }

    public static FreecamKeyMappingBuilder builder(String translationKey) {
        return new FreecamKeyMappingBuilder(translationKey);
    }

    public FreecamKeyMappingBuilder type(InputConstants.Type type) {
        this.type = type;
        return this;
    }

    public FreecamKeyMappingBuilder maxHoldTicks(long ticks) {
        this.maxTicks = ticks;
        return this;
    }

    public FreecamKeyMappingBuilder defaultKey(int keyCode) {
        this.keyCode = keyCode;
        return this;
    }

    public FreecamKeyMappingBuilder action(Runnable action) {
        this.action = action;
        return this;
    }

    public FreecamKeyMappingBuilder holdAction(HoldAction action) {
        holdAction = action;
        return this;
    }

    // With both actions, action runs on release unless held past maxHoldTicks or a hold action returned true.
    public FreecamKeyMapping build() {
        if (action != null && holdAction != null) {
            return new FreecamComboKeyMapping(translationKey, type, keyCode, action, holdAction, maxTicks);
        }
        if (action != null) {
            return new FreecamKeyMapping(translationKey, type, keyCode, self -> {
                while (self.consumeClick()) {
                    action.run();
                }
            });
        }
        if (holdAction != null) {
            return new FreecamKeyMapping(translationKey, type, keyCode, self -> {
                if (self.isDown()) {
                    holdAction.run();
                }
            });
        }
        throw new IllegalStateException("No action defined.");
    }
}
