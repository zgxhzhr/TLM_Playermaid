package io.github.zgxhzhr.playermaid.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.zgxhzhr.playermaid.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 「魂符收容」维度的区块生成器。
 *
 * <p>在固定坐标生成 18×18×18 的基岩外壳、内衬一层去皮橡木，
 * 内部为 16×16×16 的空腔（可活动范围）；其余世界全为空气。
 * 生物群系固定为虚空生物群系：没有刷怪设置、没有结构、没有装饰，
 * 因此维度内不会自然生成任何实体。该生成器经
 * {@link Registries#CHUNK_GENERATOR} 注册，维度数据可随存档正常保存/加载。</p>
 */
public class ContainmentChunkGenerator extends ChunkGenerator {

    /** 生成器类型 id（写入 CHUNK_GENERATOR 注册表，用于存档编解码）。 */
    public static final ResourceLocation ID =
            new ResourceLocation(Constants.MOD_ID, "containment_chunk_generator");

    public static final Codec<ContainmentChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ContainmentChunkGenerator::getBiomeSource)
    ).apply(instance, instance.stable(ContainmentChunkGenerator::new)));

    /** 房间外壳范围（18×18×18）。 */
    private static final int SHELL_MIN_X = -9;
    private static final int SHELL_MAX_X = 8;
    private static final int SHELL_MIN_Y = -1;
    private static final int SHELL_MAX_Y = 16;
    private static final int SHELL_MIN_Z = -9;
    private static final int SHELL_MAX_Z = 8;

    public ContainmentChunkGenerator(Registry<Biome> biomes) {
        this(new FixedBiomeSource(biomes.getHolderOrThrow(Biomes.THE_VOID)));
    }

    public ContainmentChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /** 房间内指定坐标的方块；房间范围之外一律为空气。 */
    static BlockState blockAt(int x, int y, int z) {
        if (x < SHELL_MIN_X || x > SHELL_MAX_X || y < SHELL_MIN_Y || y > SHELL_MAX_Y
                || z < SHELL_MIN_Z || z > SHELL_MAX_Z) {
            return Blocks.AIR.defaultBlockState();
        }
        boolean shell = x == SHELL_MIN_X || x == SHELL_MAX_X
                || y == SHELL_MIN_Y || y == SHELL_MAX_Y
                || z == SHELL_MIN_Z || z == SHELL_MAX_Z;
        if (shell) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        // 内衬一层去皮橡木：第二层外壳，围出 16×16×16 的空腔
        boolean oak = x == SHELL_MIN_X + 1 || x == SHELL_MAX_X - 1
                || y == SHELL_MIN_Y + 1 || y == SHELL_MAX_Y - 1
                || z == SHELL_MIN_Z + 1 || z == SHELL_MAX_Z - 1;
        if (oak) {
            return Blocks.STRIPPED_OAK_WOOD.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // 无雕刻
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random,
                             ChunkAccess chunk) {
        // 无地表
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // 无自然刷怪
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            states[i] = Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        // 无调试信息
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = SHELL_MIN_Y; y <= SHELL_MAX_Y; y++) {
                    BlockState state = blockAt(x, y, z);
                    if (!state.isAir()) {
                        chunk.setBlockState(pos.set(x, y, z), state, false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }
}
