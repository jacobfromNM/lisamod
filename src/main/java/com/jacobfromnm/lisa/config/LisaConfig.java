package com.jacobfromnm.lisa.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * LisaConfig — centralised configuration for the Lisa mod (Forge 1.19.2).
 *
 * <p>
 * All values live in {@code lisa-common.toml} inside the game's {@code config/}
 * folder and can be edited by server admins or modpack creators without
 * touching
 * any code. Forge automatically reads and validates the file on startup.
 * </p>
 *
 * <p>
 * Values are grouped into two sections:
 * <ul>
 * <li><b>spawning</b> — when and how often Lisa can appear</li>
 * <li><b>phase2</b> — behaviour once she teleports behind the player</li>
 * </ul>
 * </p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
public class LisaConfig {

        // -------------------------------------------------------------------------
        // The compiled spec — Forge validates the toml file against this at load time
        // -------------------------------------------------------------------------

        /** The finished config spec handed to Forge during mod initialisation. */
        public static final ForgeConfigSpec SPEC;

        // -------------------------------------------------------------------------
        // Spawning settings
        // -------------------------------------------------------------------------

        /** Probability (0–1) that Lisa spawns near a player each check interval. */
        public static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;

        /** Minimum ticks that must pass between two Lisa spawns for the same player. */
        public static final ForgeConfigSpec.IntValue SPAWN_COOLDOWN_TICKS;

        /** Whether Lisa is allowed to spawn during the night (time 13000–23000). */
        public static final ForgeConfigSpec.BooleanValue ENABLE_NIGHT_SPAWN;

        /** Whether Lisa is allowed to spawn underground (sky light level = 0). */
        public static final ForgeConfigSpec.BooleanValue ENABLE_CAVE_SPAWN;

        // -------------------------------------------------------------------------
        // Phase 2 settings
        // -------------------------------------------------------------------------

        /** Ticks Lisa spends lurking before rolling to teleport behind the player. */
        public static final ForgeConfigSpec.IntValue PHASE2_DELAY_TICKS;

        /**
         * Probability (0–1) that the phase-2 teleport actually occurs after the delay.
         */
        public static final ForgeConfigSpec.DoubleValue PHASE2_CHANCE;

        /**
         * Ticks Lisa waits behind the player before silently despawning if never
         * spotted.
         */
        public static final ForgeConfigSpec.IntValue PHASE2_LINGER_TICKS;

        /** Damage dealt to the player when they look directly at Lisa in phase 2. */
        public static final ForgeConfigSpec.DoubleValue LOOK_DAMAGE;

        // -------------------------------------------------------------------------
        // Ambient stalk settings
        // -------------------------------------------------------------------------

        /**
         * Probability (0–1) that Lisa enters ambient-stalk mode when she spawns.
         * In this mode she silently tracks the player from behind, plays one ambient
         * sound, then vanishes — no phase-2 threat.
         */
        public static final ForgeConfigSpec.DoubleValue AMBIENT_STALK_CHANCE;

        /**
         * Ticks Lisa remains directly behind the player during an ambient stalk
         * before despawning. Should be long enough to cover the longest ambient
         * sound file (~6 seconds = 120 ticks).
         */
        public static final ForgeConfigSpec.IntValue AMBIENT_STALK_DURATION_TICKS;

        // -------------------------------------------------------------------------
        // Atmosphere settings
        // -------------------------------------------------------------------------

        /** Whether to apply the Darkness effect to the player when Lisa is in range. */
        public static final ForgeConfigSpec.BooleanValue ENABLE_DARKNESS_EFFECT;

        /** Block radius within which the Darkness effect is applied. */
        public static final ForgeConfigSpec.DoubleValue DARKNESS_RANGE;

        /** Whether Lisa periodically flickers invisible during lurking and phase 2. */
        public static final ForgeConfigSpec.BooleanValue ENABLE_FLICKER;

        // -------------------------------------------------------------------------
        // Debug logging
        // -------------------------------------------------------------------------

        /** When true, detailed debug messages are written to the server log. */
        public static final ForgeConfigSpec.BooleanValue ENABLE_LOGGING;

        // -------------------------------------------------------------------------
        // Static initialiser — builds the spec and assigns all value handles
        // -------------------------------------------------------------------------

        static {
                // Builder accumulates all value definitions, then produces an immutable spec
                ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

                // --- Spawning section ------------------------------------------------
                builder.comment("Spawning conditions").push("spawning");

                SPAWN_CHANCE = builder
                                .comment("Chance per check (every 100 ticks) that Lisa will spawn near a qualifying player.",
                                                "Note: Range: 0.0 to 1.0  (Default: 0.001 (rare))")
                                .defineInRange("spawnChance", 0.001, 0.0, 1.0);

                SPAWN_COOLDOWN_TICKS = builder
                                .comment("Minimum ticks between Lisa spawn events per player.",
                                                "Note: 48000 ticks = ~40 in-game minutes at 20 TPS",
                                                "Default: 48000")
                                .defineInRange("spawnCooldownTicks", 48000, 0, Integer.MAX_VALUE);

                ENABLE_NIGHT_SPAWN = builder
                                .comment("Allow Lisa to spawn at night (world time 13000–23000).", "Default: true")
                                .define("enableNightSpawn", true);

                ENABLE_CAVE_SPAWN = builder
                                .comment("Allow Lisa to spawn underground (sky light level 0).", "Default: true")
                                .define("enableCaveSpawn", true);

                builder.pop(); // end "spawning" section

                // --- Phase 2 section -------------------------------------------------
                builder.comment("Phase 2 — teleporting behind the player").push("phase2");

                PHASE2_DELAY_TICKS = builder
                                .comment("Ticks Lisa spends lurking before rolling to appear behind the player.",
                                                "Note: 400 ticks = 20 seconds  (Default: 400)")
                                .defineInRange("phase2DelayTicks", 400, 0, Integer.MAX_VALUE);

                PHASE2_CHANCE = builder
                                .comment("Chance that Lisa teleports behind the player after the lurk delay.",
                                                "Note: Range: 0.0 to 1.0  (Default: 0.05 (5%))")
                                .defineInRange("phase2Chance", 0.05, 0.0, 1.0);

                PHASE2_LINGER_TICKS = builder
                                .comment("Ticks Lisa stays directly behind the player before despawning if never spotted.",
                                                "Note: 260 ticks = 13 seconds (Default: 260)")
                                .defineInRange("phase2LingerTicks", 260, 0, Integer.MAX_VALUE);

                LOOK_DAMAGE = builder
                                .comment("Damage dealt when the player looks at Lisa during phase 2.",
                                                "Note: 30.0 = 15.0 hearts (Default: 30.0)")
                                .defineInRange("lookDamage", 30.0, 0.0, 1000.0);

                builder.pop(); // end "phase2" section

                // --- Ambient stalk section -------------------------------------------
                builder.comment("Ambient stalk — harmless behind-the-player encounter").push("ambientStalk");

                AMBIENT_STALK_CHANCE = builder
                                .comment("Chance that Lisa enters ambient-stalk mode when she spawns (instead of normal lurking).",
                                                "In this mode she silently tracks the player from behind, plays one ambient sound, then vanishes.",
                                                "Note: Range: 0.0 to 1.0  (Default: 0.10 (10%))")
                                .defineInRange("ambientStalkChance", 0.10, 0.0, 1.0);

                AMBIENT_STALK_DURATION_TICKS = builder
                                .comment("Ticks Lisa stays behind the player during an ambient stalk before despawning.",
                                                "Should cover the length of the longest ambient sound file (~6 seconds).",
                                                "Note: 130 ticks = 6.5 seconds  (Default: 130)")
                                .defineInRange("ambientStalkDurationTicks", 130, 1, Integer.MAX_VALUE);

                builder.pop(); // end "ambientStalk" section

                // --- Atmosphere section ----------------------------------------------
                builder.comment("Atmosphere — darkness and flicker effects").push("atmosphere");

                ENABLE_DARKNESS_EFFECT = builder
                                .comment("Apply the Darkness effect to the player while Lisa is within darknessRange.",
                                                "Default: true")
                                .define("enableDarknessEffect", true);

                DARKNESS_RANGE = builder
                                .comment("Block radius within which the Darkness effect activates.",
                                                "Amplifier scales with proximity: full range = amplifier 0, half range = amplifier 1.",
                                                "Note: Range: 0.0 to 256.0  (Default: 30.0)")
                                .defineInRange("darknessRange", 30.0, 0.0, 256.0);

                ENABLE_FLICKER = builder
                                .comment("Lisa erratically skips rendering on ~70% of frames when enabled,",
                                                "producing a rapid random blink effect (same as the Click mod's CreepingEntity).",
                                                "Default: true")
                                .define("enableFlicker", true);

                builder.pop(); // end "atmosphere" section

                // --- Logging section -------------------------------------------------
                builder.comment("Debug logging").push("logging");

                ENABLE_LOGGING = builder
                                .comment("Enable detailed debug logging for Lisa events.",
                                                "Default: false")
                                .define("enableLogging", false);

                builder.pop(); // end "logging" section

                // Seal the spec — no more values can be added after this
                SPEC = builder.build();
        }
}
