package com.zskv.fATFSCore.minigames.HideSeek;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.World;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Objects;
import java.util.logging.Level;

public class HideSeekMapGenerator {

    private enum Direction { NORTH, EAST, SOUTH, WEST }

    private final FATFSCore plugin;
    private final HideSeekConfig config;
    private final Random rng = new Random();

    private BlockVector3 mapMin;
    private BlockVector3 mapMax;
    private World mapWorld;

    public HideSeekMapGenerator(FATFSCore plugin, HideSeekConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void generate() {
        World world = plugin.getServer().getWorld(config.getWorldName());
        if (world == null) {
            plugin.getLogger().severe("Hide & Seek world '" + config.getWorldName() + "' isn't loaded, can't generate map.");
            return;
        }

        if (mapMin != null) {
            clear();
        }

        Location centerLoc = config.getSpawnLocation();
        BlockVector3 centerPos = BlockVector3.at(centerLoc.getBlockX(), centerLoc.getBlockY(), centerLoc.getBlockZ());
        int gap = config.getDoorGap();

        Clipboard spawnClipboard = loadClipboard(config.getSpawnSchematic());
        if (spawnClipboard == null) return;
        paste(world, spawnClipboard, centerPos);
        trackBounds(world, spawnClipboard, centerPos);
        FaceDistances spawnFaces = getFaceDistances(spawnClipboard);

        // need 8 distinct outer rooms now: N, E, S, W, NW, NE, SW, SE
        List<String> pool = new ArrayList<>(config.getPool());
        Collections.shuffle(pool, rng);

        if (pool.size() < 8) {
            plugin.getLogger().warning("Only " + pool.size() + " room schematics available, need 8 to fill the full grid.");
        }

        PlacedRoom north = placeNextTo(world, pool, 0, centerPos, spawnFaces, gap, Direction.NORTH);
        placeNextTo(world, pool, 1, centerPos, spawnFaces, gap, Direction.EAST);
        PlacedRoom south = placeNextTo(world, pool, 2, centerPos, spawnFaces, gap, Direction.SOUTH);
        placeNextTo(world, pool, 3, centerPos, spawnFaces, gap, Direction.WEST);

        // corners hang off the room next to them, not off spawn directly
        if (north != null) {
            placeNextTo(world, pool, 4, north.pos(), north.faces(), gap, Direction.WEST); // NW
            placeNextTo(world, pool, 5, north.pos(), north.faces(), gap, Direction.EAST); // NE
        }
        if (south != null) {
            placeNextTo(world, pool, 6, south.pos(), south.faces(), gap, Direction.WEST); // SW
            placeNextTo(world, pool, 7, south.pos(), south.faces(), gap, Direction.EAST); // SE
        }

        buildBorder(world);
    }

    // corner rooms call this with north/south's position instead of spawn's
    private PlacedRoom placeNextTo(World world, List<String> pool, int index,
                                   BlockVector3 basePos, FaceDistances baseFaces,
                                   int gap, Direction dir) {
        if (index >= pool.size()) return null;

        Clipboard clipboard = loadClipboard(pool.get(index));
        if (clipboard == null) return null;

        FaceDistances faces = getFaceDistances(clipboard);
        BlockVector3 pos = switch (dir) {
            case NORTH -> basePos.add(centerShift(baseFaces.centerBiasX(), faces.centerBiasX()), 0,
                    -(baseFaces.north() + gap + faces.south()));
            case SOUTH -> basePos.add(centerShift(baseFaces.centerBiasX(), faces.centerBiasX()), 0,
                    baseFaces.south() + gap + faces.north());
            case EAST -> basePos.add(baseFaces.east() + gap + faces.west(), 0,
                    centerShift(baseFaces.centerBiasZ(), faces.centerBiasZ()));
            case WEST -> basePos.add(-(baseFaces.west() + gap + faces.east()), 0,
                    centerShift(baseFaces.centerBiasZ(), faces.centerBiasZ()));
        };

        paste(world, clipboard, pos);
        trackBounds(world, clipboard, pos);

        return new PlacedRoom(pos, faces);
    }

    private int centerShift(int baseBias, int nextBias) {
        int diff = baseBias - nextBias;
        if (diff % 2 != 0) {
            plugin.getLogger().warning("Room centers can't perfectly align (mismatched schematic widths): off by half a block.");
        }
        return diff / 2;
    }

    public void clear() {
        if (mapMin == null || mapMax == null || mapWorld == null) return;

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(mapWorld))
                .build()) {

            CuboidRegion region = new CuboidRegion(mapMin, mapMax);
            editSession.setBlocks(region, Objects.requireNonNull(BlockTypes.AIR).getDefaultState());

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear Hide & Seek map", e);

        } finally {
            mapMin = null;
            mapMax = null;
            mapWorld = null;
        }
    }

    private void trackBounds(World world, Clipboard clipboard, BlockVector3 pastePos) {
        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 min = pastePos.add(clipboard.getRegion().getMinimumPoint().subtract(origin));
        BlockVector3 max = pastePos.add(clipboard.getRegion().getMaximumPoint().subtract(origin));

        mapWorld = world;
        mapMin = (mapMin == null) ? min : mapMin.getMinimum(min);
        mapMax = (mapMax == null) ? max : mapMax.getMaximum(max);
    }

    private Clipboard loadClipboard(String schematicName) {
        File schemFile = new File(plugin.getDataFolder(), "schematics/" + schematicName);
        if (!schemFile.exists()) {
            plugin.getLogger().severe("Missing schematic: " + schematicName);
            return null;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
        if (format == null) {
            plugin.getLogger().severe("Couldn't figure out format for " + schematicName + ", is it a valid .schem?");
            return null;
        }

        try (FileInputStream fis = new FileInputStream(schemFile);
             ClipboardReader reader = format.getReader(fis)) {
            return reader.read();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read " + schematicName + ": " + e);
            return null;
        }
    }

    private void buildBorder(World world) {
        if (mapMin == null || mapMax == null) return;

        int margin = config.getBorderMargin();
        int thickness = config.getBorderThickness();
        int heightPad = config.getBorderHeightPadding();

        BlockVector3 innerMin = mapMin.subtract(margin, 0, margin);
        BlockVector3 innerMax = mapMax.add(margin, 0, margin);

        int yMin = mapMin.y() - heightPad;
        int yMax = mapMax.y() + heightPad;

        BlockVector3 outerMin = BlockVector3.at(innerMin.x() - thickness, yMin, innerMin.z() - thickness);
        BlockVector3 outerMax = BlockVector3.at(innerMax.x() + thickness, yMax, innerMax.z() + thickness);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .build()) {

            // north/south slabs span the full outer width, so they cover the corners too
            CuboidRegion north = new CuboidRegion(
                    BlockVector3.at(outerMin.x(), yMin, outerMin.z()),
                    BlockVector3.at(outerMax.x(), yMax, innerMin.z() - 1));
            CuboidRegion south = new CuboidRegion(
                    BlockVector3.at(outerMin.x(), yMin, innerMax.z() + 1),
                    BlockVector3.at(outerMax.x(), yMax, outerMax.z()));
            // east/west only need to span the inner z range, since corners are already covered
            CuboidRegion west = new CuboidRegion(
                    BlockVector3.at(outerMin.x(), yMin, innerMin.z()),
                    BlockVector3.at(innerMin.x() - 1, yMax, innerMax.z()));
            CuboidRegion east = new CuboidRegion(
                    BlockVector3.at(innerMax.x() + 1, yMin, innerMin.z()),
                    BlockVector3.at(outerMax.x(), yMax, innerMax.z()));

            var seaLantern = Objects.requireNonNull(BlockTypes.SEA_LANTERN).getDefaultState();
            for (CuboidRegion wall : new CuboidRegion[]{north, south, east, west}) {
                editSession.setBlocks(wall, seaLantern);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to build Hide & Seek border", e);

        }

        // fold the border into the tracked bounds so a future clear() wipes it too
        mapMin = outerMin;
        mapMax = outerMax;
    }

    private void paste(World world, Clipboard clipboard, BlockVector3 pos) {
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .build()) {

            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pos)
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(operation);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to paste schematic.", e);
        }
    }

    private FaceDistances getFaceDistances(Clipboard clipboard) {
        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        BlockVector3 max = clipboard.getRegion().getMaximumPoint();

        return new FaceDistances(
                origin.x() - min.x(),
                max.x() - origin.x(),
                origin.z() - min.z(),
                max.z() - origin.z()
        );
    }

    private record FaceDistances(int west, int east, int north, int south) {
        int centerBiasX() { return east - west; }
        int centerBiasZ() { return south - north; }
    }

    private record PlacedRoom(BlockVector3 pos, FaceDistances faces) {}
}