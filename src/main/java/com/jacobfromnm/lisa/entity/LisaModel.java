package com.jacobfromnm.lisa.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * LisaModel — the 3-D model used to render Lisa in the world (Forge 1.19.2).
 *
 * <p>Extends {@link HumanoidModel}, which provides the standard six-part rig:
 * head, body, right arm, left arm, right leg, and left leg.  The geometry is
 * identical to the default player model, meaning any 64×64 player skin
 * (e.g. from minecraftskins.net) will map correctly onto {@code lisa.png}.</p>
 *
 * <p>The model goes through two steps before it can be rendered:
 * <ol>
 *   <li>{@link #createBodyLayer()} defines the geometry as a {@link LayerDefinition}
 *       and is registered with Forge via
 *       {@link com.jacobfromnm.lisa.LisaMod.ClientEvents#onRegisterLayerDefinitions}.</li>
 *   <li>Forge bakes the layer definition into a tree of {@link ModelPart} objects
 *       before the first frame, then passes the root part to
 *       {@link LisaRenderer#LisaRenderer} via {@code context.bakeLayer(ModLayers.LISA)}.</li>
 * </ol>
 * </p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
public class LisaModel extends HumanoidModel<LisaEntity> {

    /**
     * Constructs the model from a pre-baked root {@link ModelPart}.
     * Forge calls this indirectly through {@link LisaRenderer}'s constructor.
     *
     * @param root the baked model part tree produced by Forge from {@link #createBodyLayer()}
     */
    public LisaModel(ModelPart root) {
        super(root); // HumanoidModel extracts head/body/arms/legs from the root part
    }

    /**
     * Defines the geometry for Lisa's model layer.
     *
     * <p>Delegates to {@link HumanoidModel#createMesh} with no extra cube inflation
     * ({@link CubeDeformation#NONE}) and no y-offset ({@code 0.0f}), producing the
     * exact same mesh as the vanilla player.  The {@code 64, 64} arguments set the
     * texture atlas size, matching the standard player skin format.</p>
     *
     * @return the finished layer definition ready to be registered with Forge
     */
    public static LayerDefinition createBodyLayer() {
        // Build the standard humanoid mesh (head, body, arms, legs) with no inflation
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        // Wrap it in a LayerDefinition with a 64×64 texture atlas
        return LayerDefinition.create(mesh, 64, 64);
    }
}
