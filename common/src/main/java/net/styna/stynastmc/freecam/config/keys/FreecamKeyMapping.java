package net.styna.stynastmc.freecam.config.keys;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public class FreecamKeyMapping extends KeyMapping implements Tickable {

    private static final Category FREECAM_CATEGORY = Category.register(Identifier.fromNamespaceAndPath("freecam", "controls"));

    private final Consumer<FreecamKeyMapping> onTick;

    protected FreecamKeyMapping(String translationKey, InputConstants.Type type, int code) {
        this(translationKey, type, code, null);
    }

    FreecamKeyMapping(String translationKey, InputConstants.Type type, int code, Consumer<FreecamKeyMapping> onTick) {
        super("key.freecam." + translationKey, type, code, FREECAM_CATEGORY);
        this.onTick = onTick;
    }

    @Override
    public void tick() {
        onTick.accept(this);
    }

    // KeyMapping#release() doesn't work here.
    @SuppressWarnings("StatementWithEmptyBody")
    public void reset() {
        while (consumeClick()) {}
    }
}
