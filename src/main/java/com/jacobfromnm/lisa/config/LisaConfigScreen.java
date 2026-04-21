package com.jacobfromnm.lisa.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LisaConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Lisa Configuration"))
                .setSavingRunnable(LisaConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory spawn = builder.getOrCreateCategory(Component.literal("Spawn Settings"));
        spawn.addEntry(eb.startDoubleField(Component.literal("Spawn Chance"), LisaConfig.SPAWN_CHANCE.get())
                .setDefaultValue(0.01).setMin(0.0).setMax(1.0)
                .setSaveConsumer(v -> LisaConfig.SPAWN_CHANCE.set(v))
                .build());
        spawn.addEntry(eb.startIntField(Component.literal("Spawn Cooldown (ticks)"), LisaConfig.SPAWN_COOLDOWN_TICKS.get())
                .setDefaultValue(48000).setMin(0).setMax(Integer.MAX_VALUE)
                .setSaveConsumer(v -> LisaConfig.SPAWN_COOLDOWN_TICKS.set(v))
                .build());
        spawn.addEntry(eb.startBooleanToggle(Component.literal("Enable Night Spawn"), LisaConfig.ENABLE_NIGHT_SPAWN.get())
                .setDefaultValue(true)
                .setSaveConsumer(v -> LisaConfig.ENABLE_NIGHT_SPAWN.set(v))
                .build());
        spawn.addEntry(eb.startBooleanToggle(Component.literal("Enable Cave Spawn"), LisaConfig.ENABLE_CAVE_SPAWN.get())
                .setDefaultValue(true)
                .setSaveConsumer(v -> LisaConfig.ENABLE_CAVE_SPAWN.set(v))
                .build());

        ConfigCategory behavior = builder.getOrCreateCategory(Component.literal("Behavior Settings"));
        behavior.addEntry(eb.startIntField(Component.literal("Phase 2 Delay (ticks)"), LisaConfig.PHASE2_DELAY_TICKS.get())
                .setDefaultValue(400).setMin(0).setMax(Integer.MAX_VALUE)
                .setSaveConsumer(v -> LisaConfig.PHASE2_DELAY_TICKS.set(v))
                .build());
        behavior.addEntry(eb.startDoubleField(Component.literal("Phase 2 Chance"), LisaConfig.PHASE2_CHANCE.get())
                .setDefaultValue(0.01).setMin(0.0).setMax(1.0)
                .setSaveConsumer(v -> LisaConfig.PHASE2_CHANCE.set(v))
                .build());
        behavior.addEntry(eb.startIntField(Component.literal("Phase 2 Linger (ticks)"), LisaConfig.PHASE2_LINGER_TICKS.get())
                .setDefaultValue(260).setMin(0).setMax(Integer.MAX_VALUE)
                .setSaveConsumer(v -> LisaConfig.PHASE2_LINGER_TICKS.set(v))
                .build());
        behavior.addEntry(eb.startDoubleField(Component.literal("Look Damage"), LisaConfig.LOOK_DAMAGE.get())
                .setDefaultValue(30.0).setMin(0.0).setMax(1000.0)
                .setSaveConsumer(v -> LisaConfig.LOOK_DAMAGE.set(v))
                .build());
        behavior.addEntry(eb.startDoubleField(Component.literal("Ambient Stalk Chance"), LisaConfig.AMBIENT_STALK_CHANCE.get())
                .setDefaultValue(0.1).setMin(0.0).setMax(1.0)
                .setSaveConsumer(v -> LisaConfig.AMBIENT_STALK_CHANCE.set(v))
                .build());
        behavior.addEntry(eb.startIntField(Component.literal("Ambient Stalk Duration (ticks)"), LisaConfig.AMBIENT_STALK_DURATION_TICKS.get())
                .setDefaultValue(130).setMin(1).setMax(Integer.MAX_VALUE)
                .setSaveConsumer(v -> LisaConfig.AMBIENT_STALK_DURATION_TICKS.set(v))
                .build());

        ConfigCategory effects = builder.getOrCreateCategory(Component.literal("Effects Settings"));
        effects.addEntry(eb.startBooleanToggle(Component.literal("Enable Darkness Effect"), LisaConfig.ENABLE_DARKNESS_EFFECT.get())
                .setDefaultValue(true)
                .setSaveConsumer(v -> LisaConfig.ENABLE_DARKNESS_EFFECT.set(v))
                .build());
        effects.addEntry(eb.startDoubleField(Component.literal("Darkness Range"), LisaConfig.DARKNESS_RANGE.get())
                .setDefaultValue(30.0).setMin(0.0).setMax(256.0)
                .setSaveConsumer(v -> LisaConfig.DARKNESS_RANGE.set(v))
                .build());
        effects.addEntry(eb.startBooleanToggle(Component.literal("Enable Flicker"), LisaConfig.ENABLE_FLICKER.get())
                .setDefaultValue(true)
                .setSaveConsumer(v -> LisaConfig.ENABLE_FLICKER.set(v))
                .build());

        ConfigCategory logging = builder.getOrCreateCategory(Component.literal("Logging"));
        logging.addEntry(eb.startBooleanToggle(Component.literal("Enable Logging"), LisaConfig.ENABLE_LOGGING.get())
                .setDefaultValue(false)
                .setSaveConsumer(v -> LisaConfig.ENABLE_LOGGING.set(v))
                .build());

        return builder.build();
    }
}
