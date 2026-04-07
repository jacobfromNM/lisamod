package com.jacobfromnm.lisa.entity;

import com.jacobfromnm.lisa.LisaMod;
import com.jacobfromnm.lisa.config.LisaConfig;
import com.jacobfromnm.lisa.registry.ModLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * LisaRenderer — tells the client how to draw Lisa in the world (Forge 1.19.2).
 *
 * <p>Extends {@link HumanoidMobRenderer}, which handles all the standard
 * humanoid rendering pipeline: pose calculation, limb swing, head rotation,
 * and texture application.  All we need to provide is the model instance
 * and the path to the texture file.</p>
 *
 * <p>This class is only ever referenced from client-side code
 * ({@link com.jacobfromnm.lisa.LisaMod.ClientEvents}) and is therefore safe
 * to use client-only Minecraft classes without a Dist guard.</p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
public class LisaRenderer extends HumanoidMobRenderer<LisaEntity, LisaModel> {

    /**
     * The path to Lisa's skin texture inside the mod's resource pack.
     * Place your 64×64 PNG at {@code assets/lisa/textures/entity/lisa.png}.
     */
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(LisaMod.MODID, "textures/entity/lisa.png");

    /**
     * Constructs the renderer, baking the model layer and setting the shadow radius.
     *
     * @param context provides access to the baked model part tree and other render utilities
     */
    public LisaRenderer(EntityRendererProvider.Context context) {
        super(
            context,
            new LisaModel(context.bakeLayer(ModLayers.LISA)), // retrieve the baked geometry
            0.5f  // shadow radius in blocks — same as a player
        );
    }

    /**
     * Skips rendering on 70% of frames when flickering is enabled, producing the
     * same erratic blink effect used by the Click mod's CreepingEntityRenderer.
     */
    @Override
    public void render(LisaEntity entity, float yaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int light) {
        if (LisaConfig.ENABLE_FLICKER.get() && Math.random() < 0.7) {
            return;
        }
        super.render(entity, yaw, partialTicks, poseStack, buffer, light);
    }

    /**
     * Returns the texture to apply to Lisa's model each frame.
     * Called by {@link HumanoidMobRenderer} during rendering.
     *
     * @param entity the specific Lisa instance being drawn (unused here — all Lisas share one skin)
     * @return the resource location of lisa.png
     */
    @Override
    public ResourceLocation getTextureLocation(LisaEntity entity) {
        return TEXTURE;
    }
}
