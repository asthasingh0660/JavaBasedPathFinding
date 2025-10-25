/*
 Contains your current A* logic, but cleanly separated from UI:

Accepts a Grid

Returns a list of path nodes

Fires events for visualization (visited nodes, final path)

We’ll also set up event listeners for animation hooks (your yellow/blue visual updates).
 */


package src.core;

import java.util.*;

/**
 * Classic A* implementation using Grid and Node classes from core package.
 *
 * Usage:
 *   AStarAlgorithm astar = new AStarAlgorithm(grid);
 *   List<Node> path = astar.findPath(grid, start, goal);
 *   List<Node> explored = astar.getExploredNodes();
 */
public class AStarAlgorithm implements PathfindingAlgorithm {

    private final Grid grid;
    private final List<Node> exploredNodes = new ArrayList<>();

    public AStarAlgorithm(Grid grid) {
        if (grid == null) throw new IllegalArgumentException("grid cannot be null");
        this.grid = grid;
    }

    @Override
    public List<Node> findPath(Grid gridArg, Node start, Node goal) {
        Grid g = (gridArg != null) ? gridArg : this.grid;
        exploredNodes.clear();

        if (g == null || start == null || goal == null) return Collections.emptyList();
        if (!g.inBounds(start.getRow(), start.getCol()) || !g.inBounds(goal.getRow(), goal.getCol())) return Collections.emptyList();
        if (g.isWall(start.getRow(), start.getCol()) || g.isWall(goal.getRow(), goal.getCol())) return Collections.emptyList();

        // reset search state on nodes we touch (or entire grid)
        g.resetSearchState();

        // initialize start
        start.setG(0.0);
        start.setH(heuristic(start, goal));
        start.setVisitOrder(0);
        start.setExplored(true);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        open.add(start);

        Set<Node> closed = new HashSet<>();
        int visitCounter = 1;

        while (!open.isEmpty()) {
            Node current = open.poll();

            // if already processed skip
            if (closed.contains(current)) continue;

            // mark explored (visit order)
            current.setVisitOrder(visitCounter++);
            current.setExplored(true);
            exploredNodes.add(current);

            if (current.equals(goal)) {
                // reconstruct path
                List<Node> path = reconstructPath(current);
                // Ensure path nodes have visit/order flags for UI if needed
                return path;
            }

            closed.add(current);

            for (Node nbr : g.getNeighbors(current)) {
                if (closed.contains(nbr)) continue;
                double tentativeG = current.getG() + g.movementCost(current, nbr);
                boolean better = false;

                if (tentativeG < nbr.getG()) {
                    better = true;
                }

                if (better) {
                    nbr.setParent(current);
                    nbr.setG(tentativeG);
                    nbr.setH(heuristic(nbr, goal));
                    // update F via setters
                    // remove+add to PQ to update priority (PQ doesn't auto-update)
                    open.remove(nbr);
                    open.add(nbr);
                }
            }
        }

        // no path found
        return Collections.emptyList();
    }

    // Manhattan heuristic
    private double heuristic(Node a, Node b) {
        return Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getCol() - b.getCol());
    }

    // Reconstructs path from goal node back to start and returns start->...->goal order
    private List<Node> reconstructPath(Node end) {
        LinkedList<Node> path = new LinkedList<>();
        Node cur = end;
        while (cur != null) {
            path.addFirst(cur); // add to front so final order is start->goal
            if (cur.getParent() == null) break;
            cur = cur.getParent();
        }
        return path;
    }

    @Override
    public List<Node> getExploredNodes() {
        return Collections.unmodifiableList(exploredNodes);
    }
}
