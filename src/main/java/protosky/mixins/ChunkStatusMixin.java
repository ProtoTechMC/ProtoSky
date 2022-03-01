package protosky.mixins;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.gen.WorldGenUtils;
import net.minecraft.world.dimension.DimensionType;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin
{
    // LIGHT
    @Inject(method = "method_20614", at = @At("HEAD"))
    //This is really not called 'onLighting' It's a lambda. Finding 'ChunkStatus FEATURES' should work to get to it.
    private static void onLighting(ChunkStatus targetStatus, Executor executor, ServerWorld world, ChunkGenerator generator, StructureManager structureManager, ServerLightingProvider lightingProvider, Function function, List chunks, Chunk chunk, boolean bl, CallbackInfoReturnable<CompletableFuture> cir)
    {
        //if (world.getRegistryKey() == World.END) System.out.println("end");
        if((bl || !chunk.getStatus().isAtLeast(targetStatus)) && !world.getDimension().hasEnderDragonFight()) {
        //if(bl || !chunk.getStatus().isAtLeast(targetStatus)) {
            WorldGenUtils.clearEntities((ProtoChunk)chunk, world);
            WorldGenUtils.deleteBlocks((ProtoChunk) chunk, world);
            if (new ChunkPos(world.getSpawnPos()).equals(chunk.getPos())) {
                WorldGenUtils.genSpawnPlatform(chunk, world);
            }
            Heightmap.populateHeightmaps(chunk, EnumSet.of(Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Heightmap.Type.OCEAN_FLOOR, Heightmap.Type.WORLD_SURFACE));
        }
    }

    // SPAWN -> populateEntities
    //This is really not called 'afterPopulation' It's called 'LoadTask'
    //Also doesn't do anything.
    /*@Inject(method = "method_16566", at = @At("RETURN"))
    private static void afterPopulation(ChunkStatus chunkStatus, ServerWorld serverWorld, StructureManager structureManager, ServerLightingProvider serverLightingProvider, Function function, Chunk chunk, CallbackInfoReturnable<CompletableFuture> cir) {
        WorldGenUtils.clearEntities((ProtoChunk)chunk, serverWorld);
    }*/
}
