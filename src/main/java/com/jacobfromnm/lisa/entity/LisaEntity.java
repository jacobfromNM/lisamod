package com.jacobfromnm.lisa.entity;

import com.jacobfromnm.lisa.LisaMod;
import com.jacobfromnm.lisa.config.LisaConfig;
import com.jacobfromnm.lisa.registry.ModSounds;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * LisaEntity — the central entity of the Lisa mod (Forge 1.19.2).
 *
 * <p>
 * Lisa operates in two sequential phases:
 * </p>
 * <ol>
 * <li><b>LURKING</b> — spawns somewhere nearby, stands still, and plays
 * random creepy sounds at intervals. After a configurable delay she
 * rolls once to decide whether to advance to phase 2.</li>
 * <li><b>BEHIND_PLAYER</b> — teleports 2 blocks directly behind the player,
 * then checks every tick whether the player is looking at her. If they
 * are, she deals damage and despawns. If the linger timer expires first
 * she despawns silently.</li>
 * </ol>
 *
 * <p>
 * Lisa is fully invulnerable, non-collidable, and has no AI goals — all
 * behaviour is driven manually from {@link #tick()}.
 * </p>
 *
 * <p>
 * <b>1.19.2 difference from 1.20.x:</b> damage is applied via the static
 * {@link DamageSource#mobAttack(net.minecraft.world.entity.Entity)} method
 * rather than the instance method introduced in 1.20.x.
 * </p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
public class LisaEntity extends PathfinderMob {

    // -------------------------------------------------------------------------
    // Phase enum
    // -------------------------------------------------------------------------

    /**
     * Tracks which stage of her behaviour Lisa is currently in.
     * The ordinal is persisted to NBT so the phase survives chunk unloads.
     */
    public enum Phase {
        /** Phase 1: Standing somewhere nearby, occasionally playing ambient sounds. */
        LURKING,
        /** Phase 2: Teleported directly behind the player, waiting to be seen. */
        BEHIND_PLAYER,
        /**
         * Harmless encounter: Lisa tracks the player from 2 blocks behind while one
         * ambient sound plays, then despawns silently. Never leads to phase 2.
         */
        AMBIENT_STALK
    }

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /** Current behaviour phase; starts as LURKING when she first spawns. */
    private Phase phase = Phase.LURKING;

    /**
     * Total ticks this entity has been alive (used to trigger the phase-2 roll).
     */
    private int ticksAlive = 0;

    /**
     * Countdown until the next ambient sound; starts with a small initial delay.
     */
    private int soundCooldown = 100;

    /**
     * True once the phase-2 roll has been attempted — ensures it only fires once.
     */
    private boolean phase2Rolled = false;

    /** Ticks spent in BEHIND_PLAYER phase; used to enforce the linger timeout. */
    private int phase2Ticks = 0;

    /**
     * Guards the one-time spawn-position log so it only fires on the first tick.
     */
    private boolean spawnLogged = false;

    // --- Ambient stalk state ---

    /** Ticks spent in AMBIENT_STALK phase; used to enforce the duration limit. */
    private int ambientStalkTicks = 0;

    // --- Grab sequence state ---

    /** How many ticks elapse between the grab initiation and the damage landing. */
    private static final int GRAB_DURATION = 15; // 0.75 s

    /** True once the player has looked at Lisa and the grab sequence has begun. */
    private boolean grabbing = false;

    /** Ticks elapsed since the grab sequence started. */
    private int grabTicks = 0;

    // -------------------------------------------------------------------------
    // Constructor & attributes
    // -------------------------------------------------------------------------

    /**
     * Required constructor — Forge calls this via the entity type factory.
     *
     * @param type  the registered entity type
     * @param level the world Lisa is being spawned into
     */
    public LisaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /**
     * Defines Lisa's stat block. Called once during mod init and linked to
     * the entity type via {@link com.jacobfromnm.lisa.LisaMod#onAttributeCreate}.
     *
     * @return an attribute builder ready to be sealed with {@code .build()}
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0) // 10 hearts — never actually takes damage
                .add(Attributes.MOVEMENT_SPEED, 0.0) // no autonomous movement
                .add(Attributes.FOLLOW_RANGE, 128.0); // how far she can "see" a player
    }

    // -------------------------------------------------------------------------
    // AI goals
    // -------------------------------------------------------------------------

    /**
     * Intentionally empty — Lisa's behaviour is driven entirely from
     * {@link #tick()}. Leaving this empty means the vanilla AI system
     * never tries to move or target anything on its own.
     */
    @Override
    protected void registerGoals() {
    }

    // -------------------------------------------------------------------------
    // Main tick
    // -------------------------------------------------------------------------

    /**
     * Called every game tick (20×/second) by the server.
     * Advances internal timers and delegates to the appropriate phase handler.
     *
     * @implNote The {@code isClientSide} guard ensures all logic runs only on
     *           the server so we never accidentally double-spawn or double-damage.
     */
    @Override
    public void tick() {
        super.tick(); // handles base entity physics (gravity, etc.)

        // All gameplay logic is server-side only
        if (this.level.isClientSide)
            return;

        if (!spawnLogged) {
            spawnLogged = true;
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Spawned at {}", this.blockPosition());
        }

        ticksAlive++;
        soundCooldown--;

        // If no player is within 128 blocks, Lisa has no reason to exist
        Player player = this.level.getNearestPlayer(this, 128.0);
        if (player == null) {
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] No player within range — despawning at {}", this.blockPosition());
            this.discard(); // removes the entity cleanly from the world
            return;
        }

        // Always face the player regardless of phase
        facePlayer(player);

        // Apply darkness effect each tick
        applyDarknessEffect(player);

        // Resolve grab sequence before phase dispatch so it fires from any phase
        if (grabbing) {
            grabTicks++;
            if (grabTicks >= GRAB_DURATION) {
                if (LisaConfig.ENABLE_LOGGING.get())
                    LisaMod.LOGGER.info("[Lisa] Killed player '{}' — despawning", player.getName().getString());
                player.kill();
                this.discard();
            }
            return;
        }

        // Delegate to whichever phase is active
        switch (phase) {
            case LURKING -> handleLurking(player);
            case BEHIND_PLAYER -> handleBehindPlayer(player);
            case AMBIENT_STALK -> handleAmbientStalk(player);
        }
    }

    // -------------------------------------------------------------------------
    // Phase 1 — Lurking
    // -------------------------------------------------------------------------

    /**
     * Handles all behaviour while Lisa is lurking nearby.
     * Plays ambient sounds on a random interval, then rolls once for phase 2
     * after the configured delay has elapsed.
     *
     * @param player the nearest player within range
     */
    private void handleLurking(Player player) {
        // Proximity trigger: player got within 3 blocks of Lisa while she was lurking
        if (this.distanceTo(player) <= 3.0) {
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Player '{}' too close during LURKING — triggering grab",
                        player.getName().getString());
            triggerGrab(player);
            return;
        }

        // On the very first tick, roll to decide if this becomes a harmless
        // ambient-stalk event
        if (ticksAlive == 1) {
            double roll = this.random.nextDouble();
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Ambient stalk roll: {} (threshold: {})",
                        String.format("%.4f", roll), LisaConfig.AMBIENT_STALK_CHANCE.get());
            if (roll < LisaConfig.AMBIENT_STALK_CHANCE.get()) {
                enterAmbientStalk(player);
                return;
            }
        }

        // Play a random creepy sound when the cooldown expires, then reset it
        if (soundCooldown <= 0) {
            playRandomAmbientSound();
            soundCooldown = 200 + this.random.nextInt(400); // wait 10–30 seconds before next sound
        }

        // Once the lurk delay is up, roll exactly once to decide her fate
        if (!phase2Rolled && ticksAlive >= LisaConfig.PHASE2_DELAY_TICKS.get()) {
            phase2Rolled = true; // flag so this block never runs again

            double roll = this.random.nextDouble();
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Phase 2 roll: {} (threshold: {})", String.format("%.4f", roll),
                        LisaConfig.PHASE2_CHANCE.get());

            if (roll < LisaConfig.PHASE2_CHANCE.get()) {
                enterPhase2(player); // she appears behind the player
            } else {
                if (LisaConfig.ENABLE_LOGGING.get())
                    LisaMod.LOGGER.info("[Lisa] Phase 2 roll failed — despawning silently");
                this.discard(); // she quietly vanishes without ever being seen
            }
        }
    }

    // -------------------------------------------------------------------------
    // Phase 2 — Behind the player
    // -------------------------------------------------------------------------

    /**
     * Transitions Lisa from LURKING to BEHIND_PLAYER.
     * Plays the appear sound and teleports her 2 blocks behind the player.
     *
     * @param player the player Lisa will appear behind
     */
    private void enterPhase2(Player player) {
        phase = Phase.BEHIND_PLAYER;

        if (LisaConfig.ENABLE_LOGGING.get())
            LisaMod.LOGGER.info("[Lisa] Entering phase 2 — teleporting behind player '{}' at {}",
                    player.getName().getString(), player.blockPosition());

        // Play the "LOOK_BEHIND_YOU" sound at the player's position so they have a
        // chance to hear it...
        this.level.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                ModSounds.LOOK_BEHIND_YOU.get(), SoundSource.HOSTILE, 2.0f, 1.0f);

        if (LisaConfig.ENABLE_LOGGING.get())
            LisaMod.LOGGER.info("[Lisa] Playing sound: look_behind_you");

        // Move Lisa to directly behind the player
        teleportBehindPlayer(player);
    }

    /**
     * Handles all behaviour while Lisa is standing behind the player.
     * Checks for the linger timeout, the escape distance, and whether the
     * player has looked directly at her.
     *
     * @param player the player Lisa is standing behind
     */
    private void handleBehindPlayer(Player player) {
        phase2Ticks++;

        // Despawn if the player never turns around within the linger window
        if (phase2Ticks > LisaConfig.PHASE2_LINGER_TICKS.get()) {
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Linger timeout expired — despawning behind player '{}'",
                        player.getName().getString());
            this.discard();
            return;
        }

        // If the player looks at Lisa — begin the grab sequence
        if (isPlayerLookingAt(player)) {
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Player '{}' looked at Lisa — beginning grab sequence",
                        player.getName().getString());
            triggerGrab(player);
        }
    }

    // -------------------------------------------------------------------------
    // Ambient stalk — harmless behind-the-player encounter
    // -------------------------------------------------------------------------

    /**
     * Transitions Lisa into AMBIENT_STALK mode.
     * Teleports her directly behind the player, then plays one sound from the
     * ambient pool at her new position. No lady or baby sounds will play during
     * this phase, and it never escalates to phase 2.
     *
     * @param player the player Lisa will track
     */
    private void enterAmbientStalk(Player player) {
        phase = Phase.AMBIENT_STALK;

        if (LisaConfig.ENABLE_LOGGING.get())
            LisaMod.LOGGER.info("[Lisa] Entering ambient stalk — teleporting behind player '{}'",
                    player.getName().getString());

        // Position her first so the sound plays at her new location
        teleportBehindPlayer(player);

        SoundEvent sound = pickFrom(ModSounds.AMBIENT_1.get(), ModSounds.AMBIENT_2.get(), ModSounds.AMBIENT_3.get());
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                sound, SoundSource.HOSTILE, 3.0f, 1.0f);

        if (LisaConfig.ENABLE_LOGGING.get())
            LisaMod.LOGGER.info("[Lisa] Playing ambient sound (ambient pool) at {}", this.blockPosition());
    }

    /**
     * Handles all behaviour while Lisa is in ambient-stalk mode.
     * Re-anchors her 2 blocks behind the player every tick so she tracks their
     * movement, then despawns silently once the configured duration expires.
     *
     * @param player the player Lisa is stalking
     */
    private void handleAmbientStalk(Player player) {
        ambientStalkTicks++;

        // Keep Lisa anchored directly behind the player as they move
        teleportBehindPlayer(player);

        if (ambientStalkTicks >= LisaConfig.AMBIENT_STALK_DURATION_TICKS.get()) {
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Ambient stalk complete — despawning behind player '{}'",
                        player.getName().getString());
            this.discard();
        }
    }

    /**
     * Applies the Darkness mob effect to the player while Lisa is within range.
     * Refreshed every tick so it persists continuously; expires naturally (~5 s)
     * once the player steps outside the configured radius.
     * Amplifier 1 is used within half the range for a deeper darkness.
     *
     * @param player the player to affect
     */
    private void applyDarknessEffect(Player player) {
        if (!LisaConfig.ENABLE_DARKNESS_EFFECT.get())
            return;
        double dist = this.distanceTo(player);
        double range = LisaConfig.DARKNESS_RANGE.get();
        if (dist <= range) {
            int amplifier = (dist <= range / 2.0) ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, amplifier, false, false));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Teleports Lisa to the position 2 blocks directly behind the player,
     * using the horizontal component of their look direction.
     *
     * @param player the player whose back Lisa will stand behind
     */
    private void facePlayer(Player player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        this.setYRot(yaw);
        this.yHeadRot = yaw;
    }

    /**
     * Initiates the grab sequence: teleports Lisa onto the player, plays the
     * scream, and applies blindness + heavy slowness. The kill fires
     * {@link #GRAB_DURATION} ticks later in {@link #tick()}.
     */
    private void triggerGrab(Player player) {
        grabbing = true;
        grabTicks = 0;
        this.teleportTo(player.getX(), player.getY(), player.getZ());
        this.level.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                ModSounds.APPEAR.get(), SoundSource.HOSTILE, 3.0f, 0.85f);
        if (LisaConfig.ENABLE_LOGGING.get())
            LisaMod.LOGGER.info("[Lisa] Playing sound: appear (grab)");
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, GRAB_DURATION, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GRAB_DURATION + 5, 5, false, false));
    }

    private void teleportBehindPlayer(Player player) {
        Vec3 look = player.getLookAngle();
        // Negate the look vector to get the "behind" direction, scale by 2 blocks
        double bx = player.getX() - look.x * 2.0;
        double by = player.getY(); // keep the same Y so she stands on the ground
        double bz = player.getZ() - look.z * 2.0;
        this.teleportTo(bx, by, bz);
    }

    /**
     * Returns {@code true} if the player's look direction is aimed within
     * roughly 14° of Lisa's eye position.
     *
     * <p>
     * Method: compute the unit vector from the player's eyes to Lisa's eyes,
     * then take the dot product with the player's look vector. A dot product
     * above 0.97 means the angle between them is less than ~14°.
     * </p>
     *
     * @param player the player to test
     * @return true if the player is looking at Lisa
     */
    private boolean isPlayerLookingAt(Player player) {
        // Direction from player's eye to Lisa's eye, normalised to length 1
        Vec3 toEntity = this.getEyePosition().subtract(player.getEyePosition()).normalize();
        // Dot product close to 1.0 = nearly the same direction = player is looking at
        // her
        return player.getLookAngle().dot(toEntity) > 0.97;
    }

    /**
     * Picks a random sound from one of the three pools (lady, baby, ambient)
     * and plays it at Lisa's current position.
     *
     * <p>
     * Volume 3.0 with a standard SoundEvent = audible up to ~48 blocks away.
     * </p>
     */
    private void playRandomAmbientSound() {
        int pool = this.random.nextInt(2);
        SoundEvent sound;
        String poolName;
        if (pool == 0) {
            sound = pickFrom(ModSounds.LADY_1.get(), ModSounds.LADY_2.get(), ModSounds.LADY_3.get(),
                    ModSounds.LADY_4.get(), ModSounds.LADY_5.get());
            poolName = "lady";
        } else {
            sound = pickFrom(ModSounds.BABY_1.get(), ModSounds.BABY_2.get(), ModSounds.BABY_3.get(),
                    ModSounds.BABY_4.get(), ModSounds.BABY_5.get());
            poolName = "baby";
        }
        if (LisaConfig.ENABLE_LOGGING.get())
            LisaMod.LOGGER.info("[Lisa] Playing ambient sound ({} pool) at {}", poolName,
                    this.blockPosition());
        this.level.playSound(null,
                this.getX(), this.getY(), this.getZ(),
                sound, SoundSource.HOSTILE, 3.0f, 1.0f); // volume 3 → ~48-block range
    }

    /**
     * Returns a random element from the provided varargs array.
     *
     * @param sounds one or more sound events to choose from
     * @return a randomly chosen sound event
     */
    private SoundEvent pickFrom(SoundEvent... sounds) {
        return sounds[this.random.nextInt(sounds.length)];
    }

    // -------------------------------------------------------------------------
    // Invulnerability & collision overrides
    // -------------------------------------------------------------------------

    /**
     * Intercepts any attack on Lisa. If a player hits her while she is LURKING,
     * the grab sequence triggers immediately. Lisa remains invulnerable regardless.
     */
    @Override
    public boolean hurt(@Nonnull DamageSource source, float amount) {
        if (phase == Phase.LURKING && !grabbing && source.getEntity() instanceof Player player) {
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Player '{}' attacked Lisa during LURKING — triggering grab",
                        player.getName().getString());
            triggerGrab(player);
        }
        return false;
    }

    /**
     * Makes Lisa completely immune to all damage sources.
     * Players cannot kill her — she only disappears on her own terms.
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    /** Prevents other entities from physically pushing Lisa around. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /** Suppresses the push response so Lisa never nudges other entities either. */
    @Override
    protected void doPush(Entity entity) {
    }

    /** Prevents the player from clicking/interacting with Lisa's hitbox. */
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /** Hides the name tag that would normally float above a mob. */
    @Override
    public boolean shouldShowName() {
        return false;
    }

    // -------------------------------------------------------------------------
    // NBT save / load (persists state across chunk unloads and world restarts)
    // -------------------------------------------------------------------------

    /**
     * Writes Lisa's runtime state into the chunk's NBT data so it survives
     * saves and chunk unloads.
     *
     * @param tag the compound tag to write into
     */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LisaPhase", phase.ordinal()); // store phase as integer (0, 1, or 2)
        tag.putInt("TicksAlive", ticksAlive);
        tag.putInt("SoundCooldown", soundCooldown);
        tag.putBoolean("Phase2Rolled", phase2Rolled);
        tag.putInt("Phase2Ticks", phase2Ticks);
        tag.putInt("AmbientStalkTicks", ambientStalkTicks);
        tag.putBoolean("Grabbing", grabbing);
        tag.putInt("GrabTicks", grabTicks);
    }

    /**
     * Reads Lisa's runtime state back from NBT when the chunk is loaded.
     *
     * @param tag the compound tag to read from
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Guard against out-of-range ordinals in case the enum ever changes
        int ord = tag.getInt("LisaPhase");
        phase = ord < Phase.values().length ? Phase.values()[ord] : Phase.LURKING;
        ticksAlive = tag.getInt("TicksAlive");
        soundCooldown = tag.getInt("SoundCooldown");
        phase2Rolled = tag.getBoolean("Phase2Rolled");
        phase2Ticks = tag.getInt("Phase2Ticks");
        ambientStalkTicks = tag.getInt("AmbientStalkTicks");
        grabbing = tag.getBoolean("Grabbing");
        grabTicks = tag.getInt("GrabTicks");
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns Lisa's current behaviour phase.
     * Primarily useful for renderer logic or debugging.
     *
     * @return the current {@link Phase}
     */
    public Phase getPhase() {
        return phase;
    }
}
