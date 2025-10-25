/*
 2️⃣ Grid.java

Handles:

Grid generation

Obstacles, start/end positions

Utility methods (reset, clear, getNeighbors, etc.)

This class is logic only — no UI.
 */


package src.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Grid logic (no UI). Manages nodes, walls, weights, start/goal, and utility methods.
 *
 * Usage:
 *   Grid grid = new Grid(rows, cols);
 *   grid.setWall(r, c, true);
 *   Node start = grid.getNodeAt(1,1);
 *   grid.setStartNode(start);
 */
public class Grid {
    private final int rows;
    private final int cols;
    private final Node[][] nodes;
    private final boolean[][] walls; // true => wall/blocked

    private Node startNode = null;
    private Node goalNode = null;

    public Grid(int rows, int cols) {
        if (rows < 1 || cols < 1) throw new IllegalArgumentException("rows/cols must be >= 1");
        this.rows = rows;
        this.cols = cols;
        this.nodes = new Node[rows][cols];
        this.walls = new boolean[rows][cols];
        initNodes();
    }

    private void initNodes() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                nodes[r][c] = new Node(r, c);
                walls[r][c] = false;
            }
        }
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    // Node access
    public Node getNodeAt(int row, int col) {
        if (!inBounds(row, col)) return null;
        return nodes[row][col];
    }

    // Walls
    public boolean isWall(int row, int col) {
        if (!inBounds(row, col)) return true;
        return walls[row][col];
    }

    public void setWall(int row, int col, boolean wall) {
        if (!inBounds(row, col)) return;
        walls[row][col] = wall;
    }

    public void toggleWall(int row, int col) {
        if (!inBounds(row, col)) return;
        walls[row][col] = !walls[row][col];
    }

    // start/goal
    public Node getStartNode() { return startNode; }
    public Node getGoalNode()  { return goalNode; }

    public void setStartNode(Node start) {
        if (start == null) { this.startNode = null; return; }
        if (!inBounds(start.getRow(), start.getCol())) throw new IllegalArgumentException("start out of bounds");
        this.startNode = start;
    }

    public void setGoalNode(Node goal) {
        if (goal == null) { this.goalNode = null; return; }
        if (!inBounds(goal.getRow(), goal.getCol())) throw new IllegalArgumentException("goal out of bounds");
        this.goalNode = goal;
    }

    public boolean inBounds(int r, int c) {
        return (r >= 0 && r < rows && c >= 0 && c < cols);
    }

    // Reset search-related state on all nodes (but keep walls and start/goal)
    public void resetSearchState() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) nodes[r][c].resetSearchState();
        }
    }

    // Reset everything (clear walls and search state; optionally keep start/goal if you want)
    public void resetAll(boolean clearStartGoal) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            walls[r][c] = false;
            nodes[r][c].resetSearchState();
            nodes[r][c].setWeight(1.0);
        }
        if (clearStartGoal) {
            startNode = null;
            goalNode = null;
        }
    }

    // neighbors (4-way). Returns only non-wall neighbor nodes.
    public List<Node> getNeighbors(Node n) {
        if (n == null) return Collections.emptyList();
        int r = n.getRow();
        int c = n.getCol();
        List<Node> nb = new ArrayList<>(4);
        int[][] del = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : del) {
            int nr = r + d[0], nc = c + d[1];
            if (!inBounds(nr, nc)) continue;
            if (walls[nr][nc]) continue;
            nb.add(nodes[nr][nc]);
        }
        return nb;
    }

    // Weighted neighbor cost (edge cost from node a to b). By default uses target node's weight.
    public double movementCost(Node from, Node to) {
        if (from == null || to == null) return Double.POSITIVE_INFINITY;
        return to.getWeight(); // 1.0 for normal grid; override if you have directional costs
    }

    // Random walls utility
    public void randomWalls(double density, long seed) {
        Random rnd = (seed == Long.MIN_VALUE) ? new Random() : new Random(seed);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                walls[r][c] = rnd.nextDouble() < density;
            }
        }
    }

    public void randomWalls(double density) {
        randomWalls(density, System.nanoTime());
    }

    // Simple pretty print (useful while debugging)
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (startNode != null && startNode.getRow() == r && startNode.getCol() == c) sb.append('S');
                else if (goalNode != null && goalNode.getRow() == r && goalNode.getCol() == c) sb.append('G');
                else if (walls[r][c]) sb.append('#');
                else sb.append('.');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
