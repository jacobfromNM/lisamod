package com.jacobfromnm.lisa.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class LisaConfig {
        public static final ForgeConfigSpec SPEC;

        private static net.minecraftforge.fml.config.ModConfig activeConfig;

        public static void onLoad(net.minecraftforge.fml.config.ModConfig config) {
                activeConfig = config;
        }

        public static void save() {
                if (activeConfig != null) activeConfig.save();
        }
        public static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;
        public static final ForgeConfigSpec.IntValue SPAWN_COOLDOWN_TICKS;
        public static final ForgeConfigSpec.BooleanValue ENABLE_NIGHT_SPAWN;
        public static final ForgeConfigSpec.BooleanValue ENABLE_CAVE_SPAWN;
        public static final ForgeConfigSpec.IntValue PHASE2_DELAY_TICKS;
        public static final ForgeConfigSpec.DoubleValue PHASE2_CHANCE;
        public static final ForgeConfigSpec.IntValue PHASE2_LINGER_TICKS;
        public static final ForgeConfigSpec.DoubleValue LOOK_DAMAGE;
        public static final ForgeConfigSpec.DoubleValue AMBIENT_STALK_CHANCE;
        public static final ForgeConfigSpec.IntValue AMBIENT_STALK_DURATION_TICKS;
        public static final ForgeConfigSpec.BooleanValue ENABLE_DARKNESS_EFFECT;
        public static final ForgeConfigSpec.DoubleValue DARKNESS_RANGE;
        public static final ForgeConfigSpec.BooleanValue ENABLE_FLICKER;
        public static final ForgeConfigSpec.BooleanValue ENABLE_LOGGING;

        static {
                ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
                builder.comment("Lisa Mod Configuration").push("general");

                builder.comment("Spawn Settings").push("spawn");
                SPAWN_CHANCE = builder
                                .comment("Chance per check (every 100 ticks) that Lisa will spawn near a qualifying player. Range: 0.0 to 1.0 (Default: 0.01)")
                                .defineInRange("spawnChance", 0.01, 0.0, 1.0);
                SPAWN_COOLDOWN_TICKS = builder
                                .comment("Minimum ticks between Lisa spawn events per player. 48000 ticks = ~40 in-game minutes at 20 TPS (Default: 48000)")
                                .defineInRange("spawnCooldownTicks", 48000, 0, Integer.MAX_VALUE);
                ENABLE_NIGHT_SPAWN = builder
                                .comment("Allow Lisa to spawn at night (world time 13000-23000). (Default: true)")
                                .define("enableNightSpawn", true);
                ENABLE_CAVE_SPAWN = builder
                                .comment("Allow Lisa to spawn underground (sky light level 0). (Default: true)")
                                .define("enableCaveSpawn", true);
                builder.pop();

                builder.comment("Behavior Settings").push("behavior");
                PHASE2_DELAY_TICKS = builder
                                .comment("Ticks Lisa spends lurking before rolling to appear behind the player. 400 ticks = 20 seconds (Default: 400)")
                                .defineInRange("phase2DelayTicks", 400, 0, Integer.MAX_VALUE);
                PHASE2_CHANCE = builder
                                .comment("Chance that Lisa teleports behind the player after the lurk delay. Range: 0.0 to 1.0 (Default: 0.01)")
                                .defineInRange("phase2Chance", 0.01, 0.0, 1.0);
                PHASE2_LINGER_TICKS = builder
                                .comment("Ticks Lisa stays directly behind the player before despawning if never spotted. 260 ticks = 13 seconds (Default: 260)")
                                .defineInRange("phase2LingerTicks", 260, 0, Integer.MAX_VALUE);
                LOOK_DAMAGE = builder
                                .comment("Damage dealt when the player looks at Lisa during phase 2. 30.0 = 15.0 hearts (Default: 30.0)")
                                .defineInRange("lookDamage", 30.0, 0.0, 1000.0);
                AMBIENT_STALK_CHANCE = builder
                                .comment("Chance that Lisa enters ambient-stalk mode when she spawns (instead of normal lurking). She silently tracks the player from behind, plays one ambient sound, then vanishes. Range: 0.0 to 1.0 (Default: 0.1)")
                                .defineInRange("ambientStalkChance", 0.1, 0.0, 1.0);
                AMBIENT_STALK_DURATION_TICKS = builder
                                .comment("Ticks Lisa stays behind the player during an ambient stalk before despawning. 130 ticks = 6.5 seconds (Default: 130)")
                                .defineInRange("ambientStalkDurationTicks", 130, 1, Integer.MAX_VALUE);
                builder.pop();

                builder.comment("Effects Settings").push("effects");
                ENABLE_DARKNESS_EFFECT = builder
                                .comment("Apply the Darkness effect to the player while Lisa is within darknessRange. (Default: true)")
                                .define("enableDarknessEffect", true);
                DARKNESS_RANGE = builder
                                .comment("Block radius within which the Darkness effect activates. Amplifier scales with proximity. Range: 0.0 to 256.0 (Default: 30.0)")
                                .defineInRange("darknessRange", 30.0, 0.0, 256.0);
                ENABLE_FLICKER = builder
                                .comment("Lisa erratically skips rendering on ~70% of frames when enabled, producing a rapid random blink effect. (Default: true)")
                                .define("enableFlicker", true);
                builder.pop();

                builder.comment("Logging Settings").push("logging");
                ENABLE_LOGGING = builder
                                .comment("Enable detailed debug logging for Lisa events. (Default: false)")
                                .define("enableLogging", false);
                builder.pop();

                builder.pop();
                SPEC = builder.build();
        }
}
