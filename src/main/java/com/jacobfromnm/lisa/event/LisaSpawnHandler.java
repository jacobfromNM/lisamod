package com.jacobfromnm.lisa.event;

import com.jacobfromnm.lisa.LisaMod;
import com.jacobfromnm.lisa.config.LisaConfig;
import com.jacobfromnm.lisa.entity.LisaEntity;
import com.jacobfromnm.lisa.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LisaSpawnHandler — decides when and where Lisa spawns near a player (Forge 1.19.2).
 *
 * <p>Listens to the {@link TickEvent.PlayerTickEvent} on the Forge event bus
 * (server-side only) and runs a multi-gate eligibility check every
 * {@value #CHECK_INTERVAL} ticks per player:</p>
 * <ol>
 *   <li>No existing Lisa already near the player</li>
 *   <li>Per-player cooldown has expired</li>
 *   <li>It is night-time or the player is underground (based on config flags)</li>
 *   <li>The random spawn-chance roll succeeds</li>
 * </ol>
 *
 * <p>If all gates pass, {@link #trySpawn} picks a random position 10–20 blocks
 * away from the player and places Lisa on the nearest solid ground.</p>
 *
 * @author jacobfromnm
 * @version 1.0.0
 */
@Mod.EventBusSubscriber(modid = LisaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LisaSpawnHandler {

    /** How often (in ticks) each player's spawn eligibility is re-evaluated. */
    private static final int CHECK_INTERVAL = 100; // every 5 seconds

    /**
     * Maps each online player's UUID to the server game-time of their last
     * successful Lisa spawn.  Used to enforce {@link LisaConfig#SPAWN_COOLDOWN_TICKS}.
     *
     * <p>Note: this map lives in memory only — it resets when the server restarts,
     * which means the cooldown does not persist across sessions.</p>
     */
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    // -------------------------------------------------------------------------
    // Event handler
    // -------------------------------------------------------------------------

    /**
     * Fires twice per player per tick (once at Phase.START, once at Phase.END).
     * We only act at Phase.END to ensure the player's position has fully updated.
     *
     * @param event contains the player reference and the tick phase
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Ignore the START phase — only process once per tick, at END
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;

        // All spawn logic is server-side only; skip on the client
        if (player.level.isClientSide) return;

        // Only check every CHECK_INTERVAL ticks to avoid per-tick overhead
        if (player.tickCount % CHECK_INTERVAL != 0) return;

        ServerLevel level = (ServerLevel) player.level;

        // Gate 1 — never spawn a second Lisa while one is already nearby
        if (hasNearbyLisa(level, player)) return;

        // Gate 2 — respect the per-player cooldown
        UUID playerId  = player.getUUID();
        long gameTime  = level.getGameTime();
        Long lastSpawn = cooldowns.get(playerId);
        if (lastSpawn != null && gameTime - lastSpawn < LisaConfig.SPAWN_COOLDOWN_TICKS.get()) return;

        // Gate 3 — check environmental conditions (night and/or cave)
        boolean isNight = isNight(level);
        boolean isCave  = isCave(player, level);

        if (!isNight && !isCave) return;                                      // neither condition met
        if (isNight  && !LisaConfig.ENABLE_NIGHT_SPAWN.get()) return;        // night spawning disabled
        if (!isNight && isCave && !LisaConfig.ENABLE_CAVE_SPAWN.get()) return; // cave spawning disabled

        // Gate 4 — random chance roll (most attempts fail here by design)
        if (level.random.nextDouble() >= LisaConfig.SPAWN_CHANCE.get()) {
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Spawn roll failed for player '{}'", player.getName().getString());
            return;
        }

        if (LisaConfig.ENABLE_LOGGING.get())
            LisaMod.LOGGER.info("[Lisa] Spawn roll passed for player '{}' — searching for spawn position", player.getName().getString());

        // All gates passed — attempt to find a valid position and spawn Lisa
        if (trySpawn(player, level)) {
            cooldowns.put(playerId, gameTime); // record the spawn time for cooldown tracking
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Spawned near player '{}' at {}", player.getName().getString(), player.blockPosition());
        } else {
            if (LisaConfig.ENABLE_LOGGING.get())
                LisaMod.LOGGER.info("[Lisa] Spawn roll passed but no valid position found near player '{}'", player.getName().getString());
        }
    }

    // -------------------------------------------------------------------------
    // Condition checks
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} during the Minecraft night cycle (ticks 13000–23000).
     *
     * @param level the server level to read the day time from
     * @return true if it is currently night
     */
    private static boolean isNight(ServerLevel level) {
        long time = level.getDayTime() % 24000; // wrap to a single 24000-tick day cycle
        return time >= 13000 && time <= 23000;
    }

    /**
     * Returns {@code true} if the player is underground (no sky-light access).
     *
     * <p>Sky light level 0 means there is no direct line of sight to the open sky
     * at the player's position — a reliable indicator of being in a cave or structure.</p>
     *
     * @param player the player to test
     * @param level  the server level
     * @return true if the player's position has zero sky light
     */
    private static boolean isCave(Player player, ServerLevel level) {
        return level.getBrightness(LightLayer.SKY, player.blockPosition()) == 0;
    }

    /**
     * Returns {@code true} if at least one Lisa entity already exists within
     * 200 blocks of the player — prevents multiple concurrent Lisas per player.
     *
     * @param level  the server level to search
     * @param player the player to check around
     * @return true if a Lisa entity is already present nearby
     */
    private static boolean hasNearbyLisa(ServerLevel level, Player player) {
        // Search a 200-block cube around the player for any LisaEntity instances
        List<LisaEntity> lisas = level.getEntitiesOfClass(
                LisaEntity.class, player.getBoundingBox().inflate(200.0));
        return !lisas.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Spawning
    // -------------------------------------------------------------------------

    /**
     * Tries up to 10 times to find a valid spawn position and place Lisa there.
     *
     * <p>Each attempt picks a random direction and a random distance of 10–20
     * blocks from the player, then calls {@link #findGround} to locate a
     * solid footing near that horizontal position.</p>
     *
     * @param player the player Lisa will spawn near
     * @param level  the server level to spawn into
     * @return true if a Lisa entity was successfully added to the world
     */
    private static boolean trySpawn(Player player, ServerLevel level) {
        for (int attempt = 0; attempt < 10; attempt++) {
            // Pick a random horizontal direction and distance
            double angle = level.random.nextDouble() * Math.PI * 2.0;  // 0–360°
            double dist  = 10.0 + level.random.nextDouble() * 10.0;    // 10–20 blocks away

            // Convert polar coordinates to world-space X/Z offsets
            int cx = (int) (player.getX() + Math.cos(angle) * dist);
            int cz = (int) (player.getZ() + Math.sin(angle) * dist);
            int cy = (int) player.getY();

            // Find a solid block to stand on near this horizontal position
            BlockPos spawnPos = findGround(level, new BlockPos(cx, cy, cz));
            if (spawnPos == null) continue; // no valid ground here — try another angle

            // Create the entity instance from the registered type
            LisaEntity lisa = ModEntities.LISA.get().create(level);
            if (lisa == null) continue; // creation failed — should never happen, but guard anyway

            // Centre Lisa on the block and face south (yaw 0, pitch 0)
            lisa.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(lisa); // add her to the world — triggers all join events
            return true;
        }
        return false; // all 10 attempts failed to find valid ground
    }

    /**
     * Searches ±10 blocks vertically from {@code origin} for a position where:
     * <ul>
     *   <li>the block below is solid (something to stand on)</li>
     *   <li>the block at the position itself is air (room for Lisa's feet)</li>
     *   <li>the block above is also air (room for Lisa's head)</li>
     * </ul>
     *
     * @param level  the server level to query block states from
     * @param origin the starting position to search around
     * @return a valid standing {@link BlockPos}, or {@code null} if none was found
     */
    private static BlockPos findGround(ServerLevel level, BlockPos origin) {
        for (int dy = -10; dy <= 10; dy++) {
            BlockPos pos = origin.above(dy); // check this Y offset
            if (!level.getBlockState(pos.below()).isAir()  // something solid underfoot
                    && level.getBlockState(pos).isAir()    // clear at foot level
                    && level.getBlockState(pos.above()).isAir()) { // clear at head level
                return pos;
            }
        }
        return null; // no valid two-block-tall gap found within the search range
    }
}
