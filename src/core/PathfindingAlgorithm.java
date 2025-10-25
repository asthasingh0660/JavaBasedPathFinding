/*
 3️⃣ PathfindingAlgorithm.java (interface)

Defines what every algorithm must implement:
 */


package src.core;

import java.util.List;

/**
 * Interface for pathfinding algorithms.
 */
public interface PathfindingAlgorithm {
    /**
     * Find path on the supplied grid from start -> goal.
     * Returns list of nodes from start (inclusive) to goal (inclusive).
     * If no path found, returns an empty list.
     */
    List<Node> findPath(Grid grid, Node start, Node goal);

    /**
     * Returns the list of nodes that were explored by the last run (in visit order).
     * If no search has been run yet, returns an empty list.
     */
    List<Node> getExploredNodes();
}
