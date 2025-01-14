package org.moon.figura.lua.api.world;

import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.avatar.AvatarManager;
import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.ReadOnlyLuaTable;
import org.moon.figura.lua.api.entity.EntityAPI;
import org.moon.figura.lua.api.entity.PlayerAPI;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaMethodOverload;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.math.NoiseGenerator;
import org.moon.figura.math.vector.FiguraVec2;
import org.moon.figura.math.vector.FiguraVec3;
import org.moon.figura.utils.EntityUtils;
import org.moon.figura.utils.LuaUtils;

import java.util.*;

@LuaWhitelist
@LuaTypeDoc(
        name = "WorldAPI",
        value = "world"
)
public class WorldAPI {

    public static final WorldAPI INSTANCE = new WorldAPI();

    public static Level getCurrentWorld() {
        return Minecraft.getInstance().level;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_biome"
    )
    public static BiomeAPI getBiome(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getBiome", x, y, z);
        return new BiomeAPI(getCurrentWorld().getBiome(pos.asBlockPos()).value(), pos.asBlockPos());
    }

    @SuppressWarnings("deprecation")
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_block_state"
    )
    public static BlockStateAPI getBlockState(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getBlockState", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        Level world = getCurrentWorld();
        if (!world.hasChunkAt(blockPos))
            return new BlockStateAPI(Blocks.AIR.defaultBlockState(), blockPos);
        return new BlockStateAPI(world.getBlockState(blockPos), blockPos);
    }

    @SuppressWarnings("deprecation")
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, FiguraVec3.class},
                            argumentNames = {"min", "max"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, FiguraVec3.class},
                            argumentNames = {"minX", "minY", "minZ", "max"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, Double.class, Double.class, Double.class},
                            argumentNames = {"min", "maxX", "maxY", "maxZ"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"minX", "minY", "minZ", "maxX", "maxY", "maxZ"}
                    )
            },
            value = "world.get_blocks"
    )
    public static List<BlockStateAPI> getBlocks(Object x, Object y, Double z, Double w, Double t, Double h) {
        Pair<FiguraVec3, FiguraVec3> pair = LuaUtils.parse2Vec3("getBlocks", x, y, z, w, t, h);
        List<BlockStateAPI> list = new ArrayList<>();

        BlockPos min = pair.getFirst().asBlockPos();
        BlockPos max = pair.getSecond().asBlockPos();
        max = new BlockPos(
                Math.min(min.getX() + 8, max.getX()),
                Math.min(min.getY() + 8, max.getY()),
                Math.min(min.getZ() + 8, max.getZ())
        );

        Level world = getCurrentWorld();
        if (!world.hasChunksAt(min, max))
            return list;

        BlockPos.betweenClosedStream(min, max).forEach(blockPos -> list.add(new BlockStateAPI(world.getBlockState(blockPos), blockPos)));
        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_redstone_power"
    )
    public static int getRedstonePower(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getRedstonePower", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        if (getCurrentWorld().getChunkAt(blockPos) == null)
            return 0;
        return getCurrentWorld().getBestNeighborSignal(blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_strong_redstone_power"
    )
    public static int getStrongRedstonePower(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getStrongRedstonePower", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        if (getCurrentWorld().getChunkAt(blockPos) == null)
            return 0;
        return getCurrentWorld().getDirectSignalTo(blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Double.class,
                            argumentNames = "delta"
                    )
            },
            value = "world.get_time"
    )
    public static double getTime(double delta) {
        return getCurrentWorld().getGameTime() + delta;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Double.class,
                            argumentNames = "delta"
                    )
            },
            value = "world.get_time_of_day"
    )
    public static double getTimeOfDay(double delta) {
        return getCurrentWorld().getDayTime() + delta;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload,
            value = "world.get_moon_phase"
    )
    public static int getMoonPhase() {
        return getCurrentWorld().getMoonPhase();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Double.class,
                            argumentNames = "delta"
                    )
            },
            value = "world.get_rain_gradient"
    )
    public static double getRainGradient(Float delta) {
        if (delta == null) delta = 1f;
        return getCurrentWorld().getRainLevel(delta);
    }

    @LuaWhitelist
    @LuaMethodDoc("world.is_thundering")
    public static boolean isThundering() {
        return getCurrentWorld().isThundering();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_light_level"
    )
    public static Integer getLightLevel(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getLightLevel", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        Level world = getCurrentWorld();
        if (world.getChunkAt(blockPos) == null)
            return null;
        world.updateSkyBrightness();
        return world.getLightEngine().getRawBrightness(blockPos, world.getSkyDarken());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_sky_light_level"
    )
    public static Integer getSkyLightLevel(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getSkyLightLevel", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        Level world = getCurrentWorld();
        if (world.getChunkAt(blockPos) == null)
            return null;
        return world.getBrightness(LightLayer.SKY, blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.get_block_light_level"
    )
    public static Integer getBlockLightLevel(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getBlockLightLevel", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        Level world = getCurrentWorld();
        if (world.getChunkAt(blockPos) == null)
            return null;
        return world.getBrightness(LightLayer.BLOCK, blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            value = "world.is_open_sky"
    )
    public static Boolean isOpenSky(Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("isOpenSky", x, y, z);
        BlockPos blockPos = pos.asBlockPos();
        Level world = getCurrentWorld();
        if (world.getChunkAt(blockPos) == null)
            return null;
        return world.canSeeSky(blockPos);
    }

    @LuaWhitelist
    @LuaMethodDoc("world.get_dimension")
    public static String getDimension() {
        Level world = getCurrentWorld();
        return world.dimension().location().toString();
    }

    @LuaWhitelist
    @LuaMethodDoc("world.get_players")
    public static Map<String, EntityAPI<?>> getPlayers() {
        HashMap<String, EntityAPI<?>> playerList = new HashMap<>();
        for (Player player : getCurrentWorld().players())
            playerList.put(player.getName().getString(), PlayerAPI.wrap(player));
        return playerList;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, FiguraVec3.class},
                            argumentNames = {"half_range", "pos"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Double.class, Double.class, Double.class},
                            argumentNames = {"half_range", "x", "y", "z"}
                    )
            },
            value = "world.get_nearby_entities"
    )
    public static Map<String, EntityAPI<?>> getNearbyEntities(Integer range, Object x, Double y, Double z) {
        FiguraVec3 pos = LuaUtils.parseVec3("getNearbyEntities", x, y, z);
        HashMap<String, EntityAPI<?>> entityList = new HashMap<>();

        AABB area = new AABB(pos.asVec3().subtract(range, range, range), pos.asVec3().add(range, range, range));
        for (Entity entity : getCurrentWorld().getEntitiesOfClass(Entity.class, area)) {
            entityList.put(entity.getName().getString(), EntityAPI.wrap(entity));
            System.out.println(entity.getName().getString());
        }
        return entityList;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "UUID"
            ),
            value = "world.get_entity"
    )
    public static EntityAPI<?> getEntity(@LuaNotNil String uuid) {
        try {
            return EntityAPI.wrap(EntityUtils.getEntityByUUID(UUID.fromString(uuid)));
        } catch (Exception ignored) {
            throw new LuaError("Invalid UUID");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class, FiguraVec3.class, FiguraVec3.class},
                            argumentNames = {"ignoreLiquids", "start", "end"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class, FiguraVec3.class, Double.class, Double.class, Double.class},
                            argumentNames = {"ignoreLiquids", "start", "w", "t", "h"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class, Double.class, Double.class, Double.class, FiguraVec3.class},
                            argumentNames = {"ignoreLiquids", "x", "y", "z", "end"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Boolean.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"ignoreLiquids", "x", "y", "z", "w", "t", "h"}
                    )
            },
            value = "world.linetraceBlock"
    )
    public HashMap<String, Object> linetraceBlock(boolean fluid, Object x, Object y, Double z, Object w, Double t, Double h) {
        FiguraVec3 start, end;

        Pair<FiguraVec3, FiguraVec3> pair = LuaUtils.parse2Vec3("linetraceBlock", x, y, z, w, t, h);
        start = pair.getFirst();
        end = pair.getSecond();

        BlockHitResult result = getCurrentWorld().clip(new ClipContext(start.asVec3(), end.asVec3(), ClipContext.Block.OUTLINE, fluid ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY, new Marker(EntityType.MARKER, getCurrentWorld())));
        if (result == null || result.getType() == HitResult.Type.MISS)
            return null;

        HashMap<String, Object> map = new HashMap<>();
        BlockPos pos = result.getBlockPos();
        map.put("direction", result.getDirection().getName());
        map.put("position", FiguraVec3.fromVec3(result.getLocation()));
        map.put("block", getBlockState(pos.getX(), (double) pos.getY(), (double) pos.getZ()));

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, FiguraVec3.class},
                            argumentNames = {"start", "end"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {FiguraVec3.class, Double.class, Double.class, Double.class},
                            argumentNames = {"start", "w", "t", "h"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, FiguraVec3.class},
                            argumentNames = {"x", "y", "z", "end"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z", "w", "t", "h"}
                    )
            },
            value = "world.linetraceEntity"
    )
    public HashMap<String, Object> linetraceEntity(Object x, Object y, Double z, Object w, Double t, Double h) {
        FiguraVec3 start, end;

        Pair<FiguraVec3, FiguraVec3> pair = LuaUtils.parse2Vec3("linetraceEntity", x, y, z, w, t, h);
        start = pair.getFirst();
        end = pair.getSecond();

        EntityHitResult result = ProjectileUtil.getEntityHitResult(Minecraft.getInstance().player != null ? Minecraft.getInstance().player : new Marker(EntityType.MARKER, getCurrentWorld()), start.asVec3(), end.asVec3(), new AABB(start.asVec3(), end.asVec3()), entity -> true, Double.MAX_VALUE);

        if (result == null)
            return null;

        HashMap<String, Object> map = new HashMap<>();
        map.put("entity", EntityAPI.wrap(result.getEntity()));
        map.put("pos", FiguraVec3.fromVec3(result.getLocation()));

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc("world.avatar_vars")
    public static Map<String, LuaTable> avatarVars() {
        HashMap<String, LuaTable> varList = new HashMap<>();
        for (Avatar avatar : AvatarManager.getLoadedAvatars()) {
            LuaTable tbl = avatar.luaRuntime == null ? new LuaTable() : avatar.luaRuntime.avatar_meta.storedStuff;
            varList.put(avatar.owner.toString(), new ReadOnlyLuaTable(tbl));
        }
        return varList;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "block"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, FiguraVec3.class},
                            argumentNames = {"block", "pos"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Double.class, Double.class, Double.class},
                            argumentNames = {"block", "x", "y", "z"}
                    )
            },
            value = "world.new_block"
    )
    public static BlockStateAPI newBlock(@LuaNotNil String string, Object x, Double y, Double z) {
        BlockPos pos = LuaUtils.parseVec3("newBlock", x, y, z).asBlockPos();
        try {
            Level level = getCurrentWorld();
            BlockState block = BlockStateArgument.block(CommandBuildContext.simple(level.registryAccess(), level.enabledFeatures())).parse(new StringReader(string)).getState();
            return new BlockStateAPI(block, pos);
        } catch (Exception e) {
            throw new LuaError("Could not parse block state from string: " + string);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {BlockStateAPI.class, FiguraVec3.class},
                            argumentNames = {"block", "pos"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {BlockStateAPI.class, Double.class, Double.class, Double.class},
                            argumentNames = {"block", "x", "y", "z"}
                    )
            },
            value = "world.set_block"
    )
    public static Boolean setBlock(BlockStateAPI state, Object x, Double y, Double z) {
        BlockPos pos = LuaUtils.parseVec3("setBlock", x, y, z).asBlockPos();
        try {
            ClientLevel level = (ClientLevel) getCurrentWorld();
            BlockState blockState = state.blockState;
            Player p = Minecraft.getInstance().player;

            if (p != null) {
                level.setBlock(pos, blockState, 5, 512);
                level.syncBlockState(pos, blockState, p.getPosition(0));
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Long.class, FiguraVec3.class, Boolean.class},
                            argumentNames = {"seed", "pos", "isSmooth"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Long.class, Double.class, Double.class, Double.class, Boolean.class},
                            argumentNames = {"seed", "xPos", "yPos", "zPos", "isSmooth"}
                    ),
            },
            value = "world.get_noise"
    )
    public static Double getNoise(Long seed, Object x, Double y, Double z, Boolean isSmooth) {
        FiguraVec3 pos = LuaUtils.parseVec3("getNoise", x, y, z);
        NoiseGenerator noise = new NoiseGenerator();
        noise.setSeed(seed);
        if (isSmooth) return noise.smoothNoise(pos.x, pos.y, pos.z);
        else { return noise.noise(pos.x,pos.y,pos.z); }
    }


    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "item"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Integer.class},
                            argumentNames = {"item", "count"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Integer.class, Integer.class},
                            argumentNames = {"item", "count", "damage"}
                    )
            },
            value = "world.new_item"
    )
    public static ItemStackAPI newItem(@LuaNotNil String string, Integer count, Integer damage) {
        try {
            Level level = getCurrentWorld();
            ItemStack item = ItemArgument.item(CommandBuildContext.simple(level.registryAccess(), level.enabledFeatures())).parse(new StringReader(string)).createItemStack(1, false);
            if (count != null)
                item.setCount(count);
            if (damage != null)
                item.setDamageValue(damage);
            return new ItemStackAPI(item);
        } catch (Exception e) {
            throw new LuaError("Could not parse item stack from string: " + string);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("world.exists")
    public static boolean exists() {
        return getCurrentWorld() != null;
    }

    @LuaWhitelist
    @LuaMethodDoc("world.get_build_height")
    public static int[] getBuildHeight() {
        Level world = getCurrentWorld();
        return new int[]{world.getMinBuildHeight(), world.getMaxBuildHeight()};
    }

    @LuaWhitelist
    @LuaMethodDoc("world.get_spawn_point")
    public static FiguraVec3 getSpawnPoint() {
        Level world = getCurrentWorld();
        return FiguraVec3.fromBlockPos(world.getSharedSpawnPos());
    }

    @Override
    public String toString() {
        return "WorldAPI";
    }
}
