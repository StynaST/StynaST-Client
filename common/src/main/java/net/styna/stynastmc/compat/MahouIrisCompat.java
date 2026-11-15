package net.styna.stynastmc.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.styna.stynastmc.StynaSTMC;

/** Gives the render pipelines Mahou Tsukai builds at runtime a shader pack program under Iris. */
public final class MahouIrisCompat {
    private static final String NAMESPACE = "mahoutsukai";
    private static final String FULLBRIGHT_SUFFIX = "_with_fullbright_pipeline";
    // Clip, fog and fae glow read uniforms a pack program would not have, so they keep their own shaders.
    private static final Set<String> TEXT_SHADERS = Set.of(
            "minecraft:core/rendertype_text", "mahoutsukai:core/passthrough", "mahoutsukai:core/fullbright");

    private static Object text;
    private static Object shadowText;
    private static Object handBright;
    private static Object entityBright;
    private static Object shadowEntity;
    private static Object handRenderer;
    private static MethodHandle handActive;
    private static boolean failed;

    private MahouIrisCompat() {
    }

    /** @return the Iris ShaderKey for this pipeline, null to leave it alone. */
    public static Object shaderKey(RenderPipeline pipeline, boolean shadow) {
        if (!resolve()) {
            return null;
        }
        if (NAMESPACE.equals(pipeline.getLocation().getNamespace())) {
            return !TEXT_SHADERS.contains(pipeline.getVertexShader().toString()) ? null : shadow ? shadowText : text;
        }
        // Mahou's glowing item models are clones of vanilla item pipelines, so they take the entity vertex format.
        return !pipeline.getLocation().getPath().endsWith(FULLBRIGHT_SUFFIX) ? null : shadow ? shadowEntity : brightEntityKey();
    }

    private static Object brightEntityKey() {
        try {
            // The hand_water program treats dark opaque models as glass, so held swords always take the solid one.
            return (boolean) handActive.invoke(handRenderer) ? handBright : entityBright;
        } catch (Throwable e) {
            return entityBright;
        }
    }

    // Reflection because common doesn't compile against Iris.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean resolve() {
        if (handActive != null) {
            return true;
        }
        if (failed) {
            return false;
        }
        try {
            Class<? extends Enum> keys = (Class<? extends Enum>) Class.forName("net.irisshaders.iris.pipeline.programs.ShaderKey");
            text = Enum.valueOf(keys, "TEXT");
            shadowText = Enum.valueOf(keys, "SHADOW_TEXT");
            handBright = Enum.valueOf(keys, "HAND_CUTOUT_BRIGHT");
            entityBright = Enum.valueOf(keys, "BLOCK_ENTITY_BRIGHT");
            shadowEntity = Enum.valueOf(keys, "SHADOW_ENTITIES_CUTOUT");
            Class<?> hand = Class.forName("net.irisshaders.iris.pathways.HandRenderer");
            handRenderer = hand.getField("INSTANCE").get(null);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            handActive = lookup.findVirtual(hand, "isActive", MethodType.methodType(boolean.class));
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            failed = true;
            StynaSTMC.LOGGER.warn("Mahou Tsukai shader support is off, Iris internals changed", e);
            return false;
        }
    }
}
