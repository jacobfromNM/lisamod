package com.jacobfromnm.lisa.registry;

import com.jacobfromnm.lisa.LisaMod;
import com.jacobfromnm.lisa.entity.LisaEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModEntities — registers all custom entity types for the Lisa mod (Forge 1.19.2).
 *
 * <p>Uses Forge's {@link DeferredRegister} pattern: entity types are declared
 * here as lazy {@link RegistryObject} handles and actually inserted into the
 * game registry when {@code ENTITY_TYPES.register(modEventBus)} is called
 * during mod construction.</p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
public class ModEntities {

    /**
     * The deferred register that queues our entity types for insertion into
     * Forge's entity-type registry under the "lisa" namespace.
     */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LisaMod.MODID);

    /**
     * The Lisa entity type.
     *
     * <p>Key properties:
     * <ul>
     *   <li>{@code sized(0.6, 1.8)} — same hitbox width/height as a player</li>
     *   <li>{@code clientTrackingRange(128)} — clients track her up to 128 blocks away</li>
     *   <li>{@code updateInterval(3)} — position sync sent every 3 ticks to save bandwidth</li>
     *   <li>{@code MobCategory.MISC} — does not count toward hostile/passive spawn caps</li>
     * </ul>
     * </p>
     */
    public static final RegistryObject<EntityType<LisaEntity>> LISA =
            ENTITY_TYPES.register("lisa", () -> EntityType.Builder
                    .<LisaEntity>of(LisaEntity::new, MobCategory.MISC) // factory method + spawn category
                    .sized(0.6f, 1.8f)          // hitbox: player-width, player-height
                    .clientTrackingRange(128)    // how far away clients keep tabs on her
                    .updateInterval(3)           // sync position every 3 ticks
                    .build("lisa"));             // internal string key used by Forge
}
