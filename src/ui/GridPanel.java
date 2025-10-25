package src.ui;

import src.core.AStarAlgorithm;
import src.core.Grid;
import src.core.Node;
import src.core.PathfindingAlgorithm;
import src.utils.MazeGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Visual grid panel. Renders grid, walls, start/goal, explored nodes and path.
 * Supports:
 *  - Left-click: toggle wall
 *  - Right-click: set Start (first) and Goal (second)
 *  - SHIFT + drag left-button: paint walls
 *  - Buttons are wired through ControlPanel
 */
public class GridPanel extends JPanel {
    private final Grid grid;
    private final int cellSize;
    private boolean painting = false;

    // animation state
    private java.util.List<Node> lastExplored = null;
    private java.util.List<Node> lastPath = null;

    public GridPanel(Grid grid, int cellSize) {
        this.grid = grid;
        this.cellSize = cellSize;
        setPreferredSize(new Dimension(grid.getCols()*cellSize, grid.getRows()*cellSize));
        setBackground(Color.WHITE);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int r = e.getY() / cellSize;
                int c = e.getX() / cellSize;
                if (!grid.inBounds(r, c)) return;

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (grid.getStartNode() == null) {
                        grid.setStartNode(grid.getNodeAt(r, c));
                        grid.setWall(r, c, false);
                    } else if (grid.getGoalNode() == null) {
                        grid.setGoalNode(grid.getNodeAt(r,c));
                        grid.setWall(r, c, false);
                    } else {
                        // move nearest of start/goal
                        Node start = grid.getStartNode();
                        Node goal = grid.getGoalNode();
                        double dToStart = Math.hypot(r - start.getRow(), c - start.getCol());
                        double dToGoal  = Math.hypot(r - goal.getRow(), c - goal.getCol());
                        if (dToStart < dToGoal) {
                            grid.setWall(start.getRow(), start.getCol(), false);
                            grid.setStartNode(grid.getNodeAt(r,c));
                            grid.setWall(r,c,false);
                        } else {
                            grid.setWall(goal.getRow(), goal.getCol(), false);
                            grid.setGoalNode(grid.getNodeAt(r,c));
                            grid.setWall(r,c,false);
                        }
                    }
                    repaint();
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.isShiftDown()) {
                        painting = true;
                        grid.setWall(r, c, true);
                    } else {
                        grid.toggleWall(r, c);
                    }
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                painting = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!painting) return;
                int r = e.getY() / cellSize;
                int c = e.getX() / cellSize;
                if (!grid.inBounds(r,c)) return;
                grid.setWall(r, c, true);
                repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int rows = grid.getRows();
        int cols = grid.getCols();

        int maxVisit = 1;
        // compute max visit for gradient if we have visited nodes
        if (lastExplored != null && lastExplored.size()>0) {
            maxVisit = lastExplored.size();
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c*cellSize;
                int y = r*cellSize;

                if (grid.isWall(r,c)) {
                    g.setColor(Color.DARK_GRAY);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillRect(x, y, cellSize, cellSize);

                // start/goal
                Node s = grid.getStartNode();
                Node go = grid.getGoalNode();
                if (s != null && s.getRow()==r && s.getCol()==c) {
                    g.setColor(new Color(34,139,34)); // start green
                    g.fillRect(x,y,cellSize,cellSize);
                } else if (go != null && go.getRow()==r && go.getCol()==c) {
                    g.setColor(new Color(178,34,34)); // goal red
                    g.fillRect(x,y,cellSize,cellSize);
                }
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(x, y, cellSize, cellSize);
            }
        }

        // draw explored gradient if present
        if (lastExplored != null) {
            int idx = 0;
            for (Node n : lastExplored) {
                if (n == null) continue;
                int x = n.getCol()*cellSize;
                int y = n.getRow()*cellSize;
                float frac = (float)idx / (float)Math.max(1, lastExplored.size());
                int alpha = 80 + (int)((1.0 - frac) * 120);
                alpha = Math.max(0, Math.min(255, alpha));
                g.setColor(new Color(255,215,0, alpha)); // gold
                g.fillRect(x, y, cellSize, cellSize);
                idx++;
            }
        }

        // draw path if present
        if (lastPath != null) {
            for (Node p : lastPath) {
                if (p == null) continue;
                if ((grid.getStartNode()!=null && p.equals(grid.getStartNode())) ||
                    (grid.getGoalNode()!=null && p.equals(grid.getGoalNode()))) continue;
                int x = p.getCol()*cellSize;
                int y = p.getRow()*cellSize;
                g.setColor(new Color(30,144,255)); // path blue
                g.fillRect(x, y, cellSize, cellSize);
            }
        }
    }

    // Public control API for ControlPanel:
    public void resetAll() {
        grid.resetAll(true);
        lastExplored = null;
        lastPath = null;
        repaint();
    }

    public void clearPath() {
        grid.resetSearchState();
        lastExplored = null;
        lastPath = null;
        repaint();
    }

    public void randomWalls(double density) {
        grid.randomWalls(density);
        repaint();
    }

    public void generateMaze() {
        MazeGenerator.generate(grid);
        repaint();
    }

    /**
     * Runs A* in a background thread and then animates explored nodes and path.
     * @param delayMs delay between animation frames (ms)
     */
    public void runAStar(int delayMs) {
        Node start = grid.getStartNode();
        Node goal = grid.getGoalNode();
        if (start == null || goal == null) {
            JOptionPane.showMessageDialog(this, "Please set a Start and a Goal (right-click).", "Missing points", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingWorker<List<Node>, Void> worker = new SwingWorker<>() {
            List<Node> explored;
            List<Node> path;
            @Override
            protected List<Node> doInBackground() {
                PathfindingAlgorithm alg = new AStarAlgorithm(grid);
                path = alg.findPath(grid, start, goal);
                explored = alg.getExploredNodes();
                return explored;
            }

            @Override
            protected void done() {
                try {
                    explored = get();
                } catch (Exception e) {
                    explored = null;
                }
                // animate explored then path
                if (explored != null && explored.size() > 0) {
                    lastExplored = new java.util.ArrayList<>();
                    Timer t = new Timer(Math.max(1, delayMs), null);
                    final int[] idx = {0};
                    t.addActionListener(evt -> {
                        if (idx[0] >= explored.size()) {
                            t.stop();
                            // after explored animation, show final full explored list then animate path
                            lastExplored = explored;
                            repaint();
                            animatePath(path, delayMs);
                            return;
                        }
                        lastExplored.add(explored.get(idx[0]));
                        idx[0]++;
                        repaint();
                    });
                    t.start();
                } else {
                    // no explored nodes (strange) — directly animate path
                    animatePath(path, delayMs);
                }
            }
        };
        worker.execute();
    }

    private void animatePath(List<Node> path, int delayMs) {
        if (path == null || path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No path found.", "Result", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        lastPath = new java.util.ArrayList<>();
        Timer t = new Timer(Math.max(10, delayMs/2), null);
        final int[] idx = {0};
        t.addActionListener(evt -> {
            if (idx[0] >= path.size()) { ((Timer)evt.getSource()).stop(); return; }
            lastPath.add(path.get(idx[0]));
            idx[0]++;
            repaint();
        });
        t.start();
    }
}



