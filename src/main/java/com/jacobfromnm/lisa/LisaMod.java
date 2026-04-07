package com.jacobfromnm.lisa;

import com.jacobfromnm.lisa.config.LisaConfig;
import com.jacobfromnm.lisa.entity.LisaEntity;
import com.jacobfromnm.lisa.entity.LisaModel;
import com.jacobfromnm.lisa.entity.LisaRenderer;
import com.jacobfromnm.lisa.registry.ModEntities;
import com.jacobfromnm.lisa.registry.ModLayers;
import com.jacobfromnm.lisa.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * LisaMod — main entry point for the Lisa mod (Forge 1.19.2).
 *
 * <p>The {@code @Mod} annotation tells Forge to instantiate this class when the
 * game loads.  The constructor wires together every registry and config the mod
 * needs.  Client-only setup (renderers, model layers) lives in the nested
 * {@link ClientEvents} class so it is never touched on a dedicated server.</p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
@Mod(LisaMod.MODID)
public class LisaMod {

    /** The mod's unique identifier — must match gradle.properties {@code mod_id}. */
    public static final String MODID = "lisa";

    /** Shared logger; use LisaMod.LOGGER.debug/info/warn from anywhere in the mod. */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Called once by Forge during game startup.
     * Registers all deferred registries, subscribes to events, and loads config.
     */
    public LisaMod() {
        // Grab the mod-specific event bus (different from the global Forge bus)
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Tell Forge about our custom entity types and sound events
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);

        // Hook into the attribute-creation event so Lisa gets her stats (health, speed, etc.)
        modEventBus.addListener(this::onAttributeCreate);

        // Register the config file so Forge generates lisa-common.toml in the config folder
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LisaConfig.SPEC);
    }

    /**
     * Assigns the stat block defined in {@link LisaEntity#createAttributes()} to
     * the Lisa entity type.  Forge fires this event after all entities are registered.
     *
     * @param event the attribute creation event provided by Forge
     */
    private void onAttributeCreate(EntityAttributeCreationEvent event) {
        // Link Lisa's entity type to her attribute set (health, speed, follow range)
        event.put(ModEntities.LISA.get(), LisaEntity.createAttributes().build());
    }

    // -------------------------------------------------------------------------
    // Client-only events
    // -------------------------------------------------------------------------

    /**
     * Holds event handlers that must only run on the client (not on a dedicated
     * server).  The {@code value = Dist.CLIENT} parameter on the annotation
     * ensures Forge skips this class entirely on dedicated servers.
     */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {

        /**
         * Registers {@link LisaRenderer} as the visual renderer for the Lisa entity.
         * Without this, the entity would be invisible on the client.
         *
         * @param event the renderer registration event provided by Forge
         */
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // Attach LisaRenderer to the Lisa entity type so the client knows how to draw her
            event.registerEntityRenderer(ModEntities.LISA.get(), LisaRenderer::new);
        }

        /**
         * Registers the humanoid model layer definition so it can be baked into a
         * {@link net.minecraft.client.model.geom.ModelPart} at render time.
         *
         * @param event the layer-definition registration event provided by Forge
         */
        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            // Tell Forge what geometry to bake for the LISA model layer (head, body, arms, legs)
            event.registerLayerDefinition(ModLayers.LISA, LisaModel::createBodyLayer);
        }
    }
}
