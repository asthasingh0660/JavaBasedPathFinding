package src.utils;

import src.core.Grid;
import src.core.Node;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Simple recursive backtracker maze generator.
 * Public static generate(Grid) method so older code that called MazeGenerator.generate(grid) works.
 */
public class MazeGenerator {

    public static void generate(Grid grid) {
        if (grid == null) return;
        // convert everything to walls first
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getCols(); c++) {
                grid.setWall(r, c, true);
            }
        }
        // choose random odd start cell if possible
        Random rnd = new Random();
        int sr = Math.max(1, rnd.nextInt(Math.max(1, grid.getRows()/2)) * 2 + 1);
        int sc = Math.max(1, rnd.nextInt(Math.max(1, grid.getCols()/2)) * 2 + 1);
        if (!grid.inBounds(sr, sc)) { sr = 1; sc = 1; }

        carve(grid, sr, sc, rnd);

        // leave existing start/goal if present (they are nodes not walls)
        if (grid.getStartNode() != null) {
            Node s = grid.getStartNode();
            grid.setWall(s.getRow(), s.getCol(), false);
        }
        if (grid.getGoalNode() != null) {
            Node g = grid.getGoalNode();
            grid.setWall(g.getRow(), g.getCol(), false);
        }
    }

    // carve passage at (r,c) and recurse
    private static void carve(Grid grid, int r, int c, Random rnd) {
        grid.setWall(r, c, false);

        Integer[] dirs = {0,1,2,3};
        List<Integer> order = Arrays.asList(dirs);
        Collections.shuffle(order, rnd);

        for (int d : order) {
            int dr = 0, dc = 0;
            if (d==0) { dr = -2; dc = 0; }
            if (d==1) { dr = 2; dc = 0; }
            if (d==2) { dr = 0; dc = -2; }
            if (d==3) { dr = 0; dc = 2; }

            int nr = r + dr;
            int nc = c + dc;
            if (!grid.inBounds(nr, nc)) continue;
            if (!grid.isWall(nr, nc)) continue;

            // knock down wall between
            grid.setWall(r + dr/2, c + dc/2, false);
            carve(grid, nr, nc, rnd);
        }
    }
}
