package com.jacobfromnm.lisa.registry;

import com.jacobfromnm.lisa.LisaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModSounds — registers all custom sound events for the Lisa mod (Forge
 * 1.19.2).
 *
 * <p>
 * Each {@link SoundEvent} registered here has a matching entry in
 * {@code assets/lisa/sounds.json} that maps it to an {@code .ogg} file.
 * The actual audio files must be placed in {@code assets/lisa/sounds/}.
 * </p>
 *
 * <p>
 * <b>1.19.2 note:</b> SoundEvent is constructed directly with
 * {@code new SoundEvent(ResourceLocation)} rather than the static factory
 * methods introduced in 1.20.x.
 * </p>
 *
 * <p>
 * Sound pools:
 * <ul>
 * <li><b>lady_1 – lady_5</b> — creepy woman sounds, played while lurking</li>
 * <li><b>baby_1 – baby_5</b> — creepy child sounds, played while lurking</li>
 * <li><b>ambient_1 – ambient_3</b> — general eerie ambient sounds</li>
 * <li><b>appear</b> — plays once when Lisa teleports behind the player</li>
 * </ul>
 * </p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
public class ModSounds {

    /**
     * Deferred register that queues sound events for insertion into the
     * Forge sound-event registry under the "lisa" namespace.
     */
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
            LisaMod.MODID);

    // -------------------------------------------------------------------------
    // Lady sounds (ogg files: assets/lisa/sounds/lady_1.ogg … lady_5.ogg)
    // -------------------------------------------------------------------------

    public static final RegistryObject<SoundEvent> LADY_1 = register("lady_1");
    public static final RegistryObject<SoundEvent> LADY_2 = register("lady_2");
    public static final RegistryObject<SoundEvent> LADY_3 = register("lady_3");
    public static final RegistryObject<SoundEvent> LADY_4 = register("lady_4");
    public static final RegistryObject<SoundEvent> LADY_5 = register("lady_5");

    // -------------------------------------------------------------------------
    // Baby sounds (ogg files: assets/lisa/sounds/baby_1.ogg … baby_5.ogg)
    // -------------------------------------------------------------------------

    public static final RegistryObject<SoundEvent> BABY_1 = register("baby_1");
    public static final RegistryObject<SoundEvent> BABY_2 = register("baby_2");
    public static final RegistryObject<SoundEvent> BABY_3 = register("baby_3");
    public static final RegistryObject<SoundEvent> BABY_4 = register("baby_4");
    public static final RegistryObject<SoundEvent> BABY_5 = register("baby_5");

    // -------------------------------------------------------------------------
    // Ambient sounds (ogg files: assets/lisa/sounds/ambient_1.ogg … ambient_3.ogg)
    // -------------------------------------------------------------------------

    public static final RegistryObject<SoundEvent> AMBIENT_1 = register("ambient_1");
    public static final RegistryObject<SoundEvent> AMBIENT_2 = register("ambient_2");
    public static final RegistryObject<SoundEvent> AMBIENT_3 = register("ambient_3");

    // -------------------------------------------------------------------------
    // Phase 2 trigger (ogg file: assets/lisa/sounds/appear.ogg)
    // -------------------------------------------------------------------------

    /** Played at the player's position the moment Lisa teleports behind them. */
    public static final RegistryObject<SoundEvent> APPEAR = register("appear");
    public static final RegistryObject<SoundEvent> LOOK_BEHIND_YOU = register("look_behind_you");

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Registers a sound event under the "lisa" namespace.
     *
     * <p>
     * <b>1.19.2 difference from 1.20.x:</b> uses
     * {@code new SoundEvent(ResourceLocation)}
     * directly. The static factory methods ({@code createVariableRangeEvent},
     * {@code createFixedRangeEvent}) do not exist in this version.
     * </p>
     *
     * @param name the sound name, e.g. {@code "lady_1"}
     * @return a lazy registry handle for the sound event
     */
    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(LisaMod.MODID, name)));
    }
}
