package protosky.mixins;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkHolder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerating;
import net.minecraft.world.chunk.ChunkGenerationContext;
import net.minecraft.world.chunk.ChunkGenerationStep;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.WorldGenUtils;
import protosky.stuctures.PillarHelper;
import protosky.stuctures.StructureHelper;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

@Mixin(ChunkGenerating.class)
public abstract class ChunkGeneratingMixin {
    @Inject(method = "generateFeatures", at = @At("HEAD"), cancellable = true)
    //This is where blocks structures should get placed, now it's where the structures ProtoSky needs get placed.
    private static void onGenerateFeatures(ChunkGenerationContext context, ChunkGenerationStep step, BoundedRegionArray<AbstractChunkHolder> chunks, Chunk chunk, CallbackInfoReturnable<CompletableFuture<Chunk>> cir) {
        ServerWorld world = context.world();
        ChunkGenerator generator = context.generator();
        Heightmap.populateHeightmaps(chunk, EnumSet.of(Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Heightmap.Type.OCEAN_FLOOR, Heightmap.Type.WORLD_SURFACE));
        ChunkRegion chunkRegion = new ChunkRegion(world, chunks, step, chunk);
        //This would normally generate structures, the blocks, not the bounding boxes.
        //context.generator().generateFeatures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion));
        //Blender.tickLeavesAndFluids(chunkRegion, chunk);

        //Generate do the structures then delete blocks while in the end to remove the end cities
        if (world.getRegistryKey() == World.END) {
            //This generates all the structures
            StructureHelper.handleStructures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion), generator, true);
            //Delete all the terrain and end cities. I couldn't figure out how to generate just the shulkers and elytra, so I resorted to just generating the whole thing and deleting the blocks.
            WorldGenUtils.deleteBlocks(chunk, world);
            //Generate the end pillars. This is its own thing and not in handleStructures() because pillars are features not structures.
            PillarHelper.generate(world, chunk);

        //Do it the other way around when generating the ow as to leave the end portal frames.
        } else {
            //We need handle some structures before we delete blocks because they rely on blocks being there.
            StructureHelper.handleStructures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion), generator, true);
            //Delete all the terrain and end cities. I couldn't figure out how to generate just the shulkers and elytra, so I resorted to just generating the whole thing and deleting the blocks.
            WorldGenUtils.deleteBlocks(chunk, world);
            //This generates all the structures
            StructureHelper.handleStructures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion), generator, false);
        }
        cir.setReturnValue(CompletableFuture.completedFuture(chunk));
    }

    @Inject(method = "initializeLight", at = @At("HEAD"), cancellable = false)
    //We need to move the heightmaps down to y = 0 after structures have been generated because some rely on the heightmap to move.
    //This used to be in the unused 'HEIGHTMAPS' status, but in 1.20 this was removed. Now we're using INITIALIZE_LIGHT.
    private static void onInitializeLight(ChunkGenerationContext context, ChunkGenerationStep step, BoundedRegionArray<AbstractChunkHolder> chunks, Chunk chunk, CallbackInfoReturnable<CompletableFuture<Chunk>> cir) {
        //Move the heightmaps down to y-64
        //This gets done here not above in FEATURES because there are multiple threads that generate features. One thread
        // may place a structure in a chunk then move the heightmap down to y=-64 when a second structure in the chunk on
        // a different thread still needs to be generated and requires the 'correct' heightmap
        //LOGGER.info("Light " + chunk.getPos());
        WorldGenUtils.genHeightMaps(chunk);

        //Don't cancel because we want the lighting to work.
        //ci.cancel();
    }

    @Inject(method = "generateEntities", at = @At("HEAD"), cancellable = true)
    //Spawning entities is skipped here. Even without this nothing would happen because entities from structures never
    // get generated because that is skipped above and non-structure entities have no blocks to spawn on so they don't spawn.
    // This is just an optimization
    private static void onGenerateEntities(ChunkGenerationContext context, ChunkGenerationStep step, BoundedRegionArray<AbstractChunkHolder> chunks, Chunk chunk, CallbackInfoReturnable<CompletableFuture<Chunk>> cir) {
        cir.setReturnValue(CompletableFuture.completedFuture(chunk));
    }
}