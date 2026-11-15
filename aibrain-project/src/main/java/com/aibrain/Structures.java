package com.aibrain;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;

/**
 * Building blueprints. Coordinates are relative to the build origin:
 * dy = 0 is the ground layer (floor / farmland), dy >= 1 is above the ground.
 */
public class Structures {

    public static final String[] NAMES = { "house", "farm", "redstone mechanism" };

    public static final class Piece {
        public final int dx, dy, dz;
        public final IBlockState state;

        Piece(int dx, int dy, int dz, IBlockState state) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.state = state;
        }
    }

    private static void put(List<Piece> l, int x, int y, int z, IBlockState s) {
        l.add(new Piece(x, y, z, s));
    }

    public static List<Piece> build(int type, int style) {
        switch (type) {
            case 0: return house(style);
            case 1: return farm(style);
            default: return redstone(style);
        }
    }

    // ------------------------------------------------------------------ house
    private static List<Piece> house(int style) {
        Block[] walls = { Blocks.PLANKS, Blocks.STONEBRICK, Blocks.BRICK_BLOCK, Blocks.QUARTZ_BLOCK };
        Block[] stairs = { Blocks.OAK_STAIRS, Blocks.STONE_BRICK_STAIRS, Blocks.BRICK_STAIRS, Blocks.QUARTZ_STAIRS };
        int s = style & 3;

        IBlockState wall = walls[s].getDefaultState();
        IBlockState stS = stairs[s].getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH);
        IBlockState stN = stairs[s].getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH);
        IBlockState log = Blocks.LOG.getDefaultState();
        IBlockState floor = Blocks.COBBLESTONE.getDefaultState();
        IBlockState glass = Blocks.GLASS_PANE.getDefaultState();
        IBlockState air = Blocks.AIR.getDefaultState();

        final int W = 7;
        final int D = 5;
        List<Piece> l = new ArrayList<Piece>();

        // clear the space
        for (int x = -1; x <= W; x++)
            for (int z = -1; z <= D; z++)
                for (int y = 1; y <= 8; y++)
                    put(l, x, y, z, air);

        // foundation and floor
        for (int x = 0; x < W; x++)
            for (int z = 0; z < D; z++) {
                put(l, x, -2, z, floor);
                put(l, x, -1, z, floor);
                put(l, x, 0, z, floor);
            }

        // walls
        for (int y = 1; y <= 3; y++) {
            for (int x = 0; x < W; x++) {
                for (int z = 0; z < D; z++) {
                    boolean edgeX = (x == 0 || x == W - 1);
                    boolean edgeZ = (z == 0 || z == D - 1);
                    if (!edgeX && !edgeZ) continue;
                    boolean corner = edgeX && edgeZ;
                    IBlockState st = corner ? log : wall;
                    if (y == 2 && !corner) {
                        if (edgeZ && (x == 2 || x == 4)) st = glass;
                        if (edgeX && z == 2) st = glass;
                    }
                    if (z == 0 && x == 3 && y <= 2) st = air; // doorway
                    put(l, x, y, z, st);
                }
            }
        }

        // ceiling
        for (int x = 0; x < W; x++)
            for (int z = 0; z < D; z++)
                put(l, x, 4, z, wall);

        // gabled roof
        for (int x = -1; x <= W; x++) {
            put(l, x, 4, -1, stS);
            put(l, x, 4, D, stN);
            put(l, x, 5, 0, stS);
            put(l, x, 5, D - 1, stN);
            put(l, x, 6, 1, stS);
            put(l, x, 6, D - 2, stN);
            put(l, x, 7, 2, wall);
        }
        // gable ends
        for (int z = 1; z <= 3; z++) {
            put(l, 0, 5, z, wall);
            put(l, W - 1, 5, z, wall);
        }
        put(l, 0, 6, 2, wall);
        put(l, W - 1, 6, 2, wall);

        // furniture
        put(l, 1, 1, 1, Blocks.TORCH.getDefaultState());
        put(l, W - 2, 1, D - 2, Blocks.TORCH.getDefaultState());
        put(l, W - 2, 1, 1, Blocks.CRAFTING_TABLE.getDefaultState());
        put(l, 1, 1, D - 2, Blocks.CHEST.getDefaultState());
        return l;
    }

    // ------------------------------------------------------------------- farm
    private static List<Piece> farm(int style) {
        Block[] crops = { Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS };
        IBlockState crop = crops[style & 3].getDefaultState();
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState farmland = Blocks.FARMLAND.getDefaultState();
        IBlockState water = Blocks.WATER.getDefaultState();
        IBlockState fence = Blocks.OAK_FENCE.getDefaultState();
        IBlockState gate = Blocks.OAK_FENCE_GATE.getDefaultState();

        final int N = 9;
        List<Piece> l = new ArrayList<Piece>();

        for (int x = -1; x <= N; x++)
            for (int z = -1; z <= N; z++)
                for (int y = 1; y <= 3; y++)
                    put(l, x, y, z, air);

        for (int x = 0; x < N; x++) {
            for (int z = 0; z < N; z++) {
                if (x == 4 && z == 4) {
                    put(l, x, 0, z, water);
                } else {
                    put(l, x, 0, z, farmland);
                    put(l, x, 1, z, crop);
                }
            }
        }

        for (int i = -1; i <= N; i++) {
            put(l, i, 1, -1, i == 4 ? gate : fence);
            put(l, i, 1, N, fence);
            put(l, -1, 1, i, fence);
            put(l, N, 1, i, fence);
        }
        put(l, -1, 2, -1, Blocks.TORCH.getDefaultState());
        put(l, N, 2, -1, Blocks.TORCH.getDefaultState());
        put(l, -1, 2, N, Blocks.TORCH.getDefaultState());
        put(l, N, 2, N, Blocks.TORCH.getDefaultState());
        return l;
    }

    // --------------------------------------------------------------- redstone
    private static List<Piece> redstone(int style) {
        int s = style & 3;
        IBlockState base = Blocks.STONEBRICK.getDefaultState();
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState lever = Blocks.LEVER.getDefaultState()
                .withProperty(BlockLever.FACING, BlockLever.EnumOrientation.UP_X);
        IBlockState dust = Blocks.REDSTONE_WIRE.getDefaultState();
        IBlockState lamp = Blocks.REDSTONE_LAMP.getDefaultState();

        int len = (s == 3) ? 10 : 6;
        List<Piece> l = new ArrayList<Piece>();

        for (int x = -1; x <= len; x++)
            for (int z = -1; z <= 2; z++)
                for (int y = 1; y <= 4; y++)
                    put(l, x, y, z, air);

        switch (s) {
            case 0: // lever -> 3 dust -> lamp
                for (int x = 0; x <= 4; x++) put(l, x, 0, 0, base);
                put(l, 0, 1, 0, lever);
                for (int x = 1; x <= 3; x++) put(l, x, 1, 0, dust);
                put(l, 4, 1, 0, lamp);
                break;
            case 1: // lever + piston that pushes a block up
                for (int x = 0; x <= 2; x++) put(l, x, 0, 0, base);
                put(l, 0, 1, 0, lever);
                put(l, 1, 1, 0, Blocks.PISTON.getDefaultState().withProperty(BlockPistonBase.FACING, EnumFacing.UP));
                put(l, 1, 2, 0, Blocks.PLANKS.getDefaultState());
                break;
            case 2: // wall of always lit lamps
                for (int x = 0; x <= 4; x++) {
                    put(l, x, 0, 0, base);
                    put(l, x, 0, 1, base);
                    put(l, x, 1, 0, lamp);
                    put(l, x, 1, 1, Blocks.REDSTONE_BLOCK.getDefaultState());
                }
                break;
            default: // lever -> long dust line -> lamp
                for (int x = 0; x <= 9; x++) put(l, x, 0, 0, base);
                put(l, 0, 1, 0, lever);
                for (int x = 1; x <= 8; x++) put(l, x, 1, 0, dust);
                put(l, 9, 1, 0, lamp);
                break;
        }
        return l;
    }
}
