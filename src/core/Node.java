/*
 1️⃣ Node.java

Represents a single cell on the grid (row, column, costs, neighbors, etc.).

Used by all algorithms (A*, Dijkstra, BFS, etc.)

Supports weights for terrain or traffic 
 */



package src.core;

import java.util.Objects;

/**
 * Represents a single cell/node on the grid.
 * Lightweight POD-style class with getters/setters used by algorithms and UI.
 */
public class Node implements Comparable<Node> {
    private final int row;
    private final int col;

    // pathfinding costs
    private double g = Double.POSITIVE_INFINITY; // cost from start
    private double h = 0.0;                       // heuristic to goal
    private double f = Double.POSITIVE_INFINITY; // g + h

    // optional weight for weighted grids (1.0 = normal)
    private double weight = 1.0;

    // parent pointer used to reconstruct path
    private Node parent = null;

    // optional fields for visualization/debugging
    private int visitOrder = -1;   // order in which node was visited (for gradients)
    private boolean explored = false;

    public Node(int row, int col) {
        this.row = row;
        this.col = col;
    }

    // position getters
    public int getRow() { return row; }
    public int getCol() { return col; }

    // g/h/f accessors
    public double getG() { return g; }
    public void setG(double g) { this.g = g; updateF(); }

    public double getH() { return h; }
    public void setH(double h) { this.h = h; updateF(); }

    public double getF() { return f; }
    private void updateF() { this.f = this.g + this.h; }

    // weight
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    // parent
    public Node getParent() { return parent; }
    public void setParent(Node parent) { this.parent = parent; }

    // visualization helpers
    public int getVisitOrder() { return visitOrder; }
    public void setVisitOrder(int order) { this.visitOrder = order; }

    public boolean isExplored() { return explored; }
    public void setExplored(boolean explored) { this.explored = explored; }

    // reset dynamic search fields (keeps position/weight)
    public void resetSearchState() {
        this.g = Double.POSITIVE_INFINITY;
        this.h = 0.0;
        this.f = Double.POSITIVE_INFINITY;
        this.parent = null;
        this.visitOrder = -1;
        this.explored = false;
    }

    @Override
    public int compareTo(Node other) {
        return Double.compare(this.f, other.f);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Node)) return false;
        Node n = (Node)o;
        return n.row == this.row && n.col == this.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return String.format("Node(%d,%d) g=%.2f h=%.2f f=%.2f w=%.2f", row, col, g, h, f, weight);
    }
}
