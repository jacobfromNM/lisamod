package com.jacobfromnm.lisa.registry;

import com.jacobfromnm.lisa.LisaMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * ModLayers — holds model layer location constants for the Lisa mod (Forge 1.19.2).
 *
 * <p>A {@link ModelLayerLocation} is essentially a named slot that the client uses
 * to look up a baked {@link net.minecraft.client.model.geom.ModelPart} at render
 * time.  The layer definition (the actual geometry) is registered in
 * {@link LisaMod.ClientEvents#onRegisterLayerDefinitions} and baked by Forge
 * before the first frame is rendered.</p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
public class ModLayers {

    /**
     * The model layer location for Lisa's main body layer.
     *
     * <p>The two constructor arguments form a unique key:
     * <ol>
     *   <li>{@code lisa:lisa} — the resource location (namespace:path)</li>
     *   <li>{@code "main"}    — the layer name within that model (e.g. "main", "armor")</li>
     * </ol>
     * This key is passed to {@code context.bakeLayer(ModLayers.LISA)} inside
     * {@link com.jacobfromnm.lisa.entity.LisaRenderer} to retrieve the ready-to-use
     * model part at construction time.</p>
     */
    public static final ModelLayerLocation LISA =
            new ModelLayerLocation(new ResourceLocation(LisaMod.MODID, "lisa"), "main");
}
