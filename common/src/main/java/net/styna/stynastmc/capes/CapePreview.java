package net.styna.stynastmc.capes;

import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Quaternionf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/** Lets a GUI skin preview draw a cape: the skin renderer mixin adds it to models attached here. */
public final class CapePreview {
    private static final Map<Model.Simple, Identifier> ATTACHED = new WeakHashMap<>();
    private static Model.Simple model;

    private CapePreview() {
    }

    public static void attach(Model.Simple body, Identifier cape) {
        if (cape == null) {
            ATTACHED.remove(body);
        } else {
            ATTACHED.put(body, cape);
        }
    }

    public static Identifier capeFor(Model.Simple body) {
        return ATTACHED.get(body);
    }

    public static Model.Simple model() {
        if (model == null) {
            ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_CAPE);
            ModelPart cape = root.getChild("body").getChild("cape");
            model = new Model.Simple(root, RenderTypes::entitySolid) {
                @Override
                public void setupAnim(Unit state) {
                    resetPose();
                    // Vanilla's resting lean with no movement.
                    cape.rotateBy(new Quaternionf().rotateY((float) -Math.PI).rotateX(6.0F * (float) (Math.PI / 180.0)).rotateY((float) Math.PI));
                }
            };
        }
        return model;
    }
}
