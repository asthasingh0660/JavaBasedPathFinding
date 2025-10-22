/*
A* Algorithm Visualizer - Single-file Java Swing application

How to use:
1. Save this file as AStarVisualizer.java
2. Compile: javac AStarVisualizer.java
3. Run: java AStarVisualizer

Controls:
- Left-click on grid: toggle wall
- Right-click on grid: set Start (first right-click) and Goal (second right-click). Right-click again to move Start/Goal.
- SHIFT + Left-click: paint walls continuously while dragging
- Buttons:
  * Run A*: starts visualization
  * Clear Path: clears only the path/visited states (keeps walls/start/goal)
  * Reset: clears everything
  * Random Walls: fill grid with random obstacles

This is intentionally a compact, single-file visualizer suitable for learning and extension.
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class AStarVisualizer extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AStarVisualizer v = new AStarVisualizer(30, 30, 20); // rows, cols, cellSize
            v.setVisible(true);
        });
    }

    private GridPanel gridPanel;

    public AStarVisualizer(int rows, int cols, int cellSize) {
        super("A* Algorithm Visualizer - Java Swing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gridPanel = new GridPanel(rows, cols, cellSize);

        JPanel control = new JPanel();
        JButton runBtn = new JButton("Run A*");
        JButton clearPathBtn = new JButton("Clear Path");
        JButton resetBtn = new JButton("Reset");
        JButton randomBtn = new JButton("Random Walls");
        JSlider speed = new JSlider(0, 200, 40);
        speed.setToolTipText("Visualization delay (ms)");

        runBtn.addActionListener(e -> gridPanel.runAStar(speed.getValue()));
        clearPathBtn.addActionListener(e -> gridPanel.clearPath());
        resetBtn.addActionListener(e -> gridPanel.resetAll());
        randomBtn.addActionListener(e -> gridPanel.randomWalls(0.3));

        control.add(runBtn);
        control.add(clearPathBtn);
        control.add(resetBtn);
        control.add(randomBtn);
        control.add(new JLabel("Delay"));
        control.add(speed);

        getContentPane().add(gridPanel, BorderLayout.CENTER);
        getContentPane().add(control, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    static class GridPanel extends JPanel {
        enum CellType { EMPTY, WALL, START, GOAL, PATH }

        class Node implements Comparable<Node> {
            int r, c;
            double g, f; // g = cost from start, f = g + h
            Node parent;
            Node(int r, int c) { this.r = r; this.c = c; g = Double.POSITIVE_INFINITY; f = Double.POSITIVE_INFINITY; }
            public int compareTo(Node o) { return Double.compare(this.f, o.f); }
            public boolean equals(Object o) {
                if (!(o instanceof Node)) return false;
                Node n = (Node) o; return n.r==r && n.c==c;
            }
            public int hashCode() { return Objects.hash(r, c); }
        }

        private int rows, cols, cellSize;
        private CellType[][] grid;
        private Node[][] nodes;
        private Node startNode = null, goalNode = null;

        // Visualization sets
        private Set<Node> closedSet = new HashSet<>();
        private Set<Node> openSet = new HashSet<>();
        private java.util.List<Node> finalPath = new ArrayList<>();

        private boolean painting = false;

        public GridPanel(int rows, int cols, int cellSize) {
            this.rows = rows; this.cols = cols; this.cellSize = cellSize;
            setPreferredSize(new Dimension(cols*cellSize, rows*cellSize));
            initGrid();
            setBackground(Color.WHITE);

            MouseAdapter ma = new MouseAdapter() {
                private boolean dragging = false;
                public void mousePressed(MouseEvent e) {
                    int r = e.getY()/cellSize;
                    int c = e.getX()/cellSize;
                    if (!inBounds(r, c)) return;
                    if (SwingUtilities.isRightMouseButton(e)) {
                        // set start/goal
                        if (startNode == null) {
                            setCell(r, c, CellType.START);
                            startNode = nodes[r][c];
                        } else if (goalNode == null) {
                            setCell(r, c, CellType.GOAL);
                            goalNode = nodes[r][c];
                        } else {
                            // move start or goal depending on proximity
                            double dToStart = Math.hypot(r - startNode.r, c - startNode.c);
                            double dToGoal  = Math.hypot(r - goalNode.r,  c - goalNode.c);
                            if (dToStart < dToGoal) {
                                setCell(startNode.r, startNode.c, CellType.EMPTY);
                                startNode = nodes[r][c];
                                setCell(r, c, CellType.START);
                            } else {
                                setCell(goalNode.r, goalNode.c, CellType.EMPTY);
                                goalNode = nodes[r][c];
                                setCell(r, c, CellType.GOAL);
                            }
                        }
                        clearSearchState();
                        repaint();
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        if (e.isShiftDown()) {
                            painting = true; dragging = true;
                            toggleWall(r, c, true);
                        } else {
                            toggleWall(r, c, false);
                        }
                        clearSearchState();
                        repaint();
                    }
                }
                public void mouseReleased(MouseEvent e) {
                    painting = false; dragging = false;
                }
                public void mouseDragged(MouseEvent e) {
                    if (!painting) return;
                    int r = e.getY()/cellSize;
                    int c = e.getX()/cellSize;
                    if (!inBounds(r, c)) return;
                    toggleWall(r, c, true);
                    clearSearchState();
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);

            initNodes();
        }

        private void initGrid() {
            grid = new CellType[rows][cols];
            for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) grid[r][c] = CellType.EMPTY;
        }
        private void initNodes() {
            nodes = new Node[rows][cols];
            for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) nodes[r][c] = new Node(r,c);
        }

        private boolean inBounds(int r, int c) { return r>=0 && r<rows && c>=0 && c<cols; }

        private void setCell(int r, int c, CellType t) { grid[r][c] = t; }

        private void toggleWall(int r, int c, boolean setWall) {
            if (grid[r][c] == CellType.START || grid[r][c] == CellType.GOAL) return;
            if (setWall) grid[r][c] = CellType.WALL; else grid[r][c] = (grid[r][c]==CellType.WALL?CellType.EMPTY:CellType.WALL);
        }

        public void resetAll() {
            for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) grid[r][c] = CellType.EMPTY;
            startNode = null; goalNode = null;
            clearSearchState();
            repaint();
        }

        public void randomWalls(double density) {
            Random rnd = new Random();
            for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
                grid[r][c] = (rnd.nextDouble() < density) ? CellType.WALL : CellType.EMPTY;
            }
            // don't overwrite start/goal
            if (startNode!=null) grid[startNode.r][startNode.c] = CellType.START;
            if (goalNode!=null) grid[goalNode.r][goalNode.c] = CellType.GOAL;
            clearSearchState();
            repaint();
        }

        public void clearPath() {
            clearSearchState();
            repaint();
        }
        private void clearSearchState() {
            closedSet.clear(); openSet.clear(); finalPath.clear();
            initNodes();
        }

        private java.util.List<Node> neighbors(Node n) {
            java.util.List<Node> list = new ArrayList<>();
            int[][] del = {{-1,0},{1,0},{0,-1},{0,1}}; // 4-way
            for (int[] d: del) {
                int nr = n.r + d[0], nc = n.c + d[1];
                if (!inBounds(nr,nc)) continue;
                if (grid[nr][nc]==CellType.WALL) continue;
                list.add(nodes[nr][nc]);
            }
            return list;
        }

        private double heuristic(Node a, Node b) {
            // Use Manhattan distance
            return Math.abs(a.r - b.r) + Math.abs(a.c - b.c);
        }

        public void runAStar(int delayMs) {
            clearSearchState();
            if (startNode==null || goalNode==null) {
                JOptionPane.showMessageDialog(this, "Please set a Start and a Goal (right-click).", "Missing points", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Prepare
            PriorityQueue<Node> openPQ = new PriorityQueue<>();
            startNode.g = 0; startNode.f = heuristic(startNode, goalNode);
            openPQ.add(startNode);
            openSet.add(startNode);

            boolean found = false;

            while (!openPQ.isEmpty()) {
                Node current = openPQ.poll();
                openSet.remove(current);
                closedSet.add(current);

                if (current.equals(goalNode)) { found = true; reconstructPath(current); break; }

                for (Node nbr : neighbors(current)) {
                    if (closedSet.contains(nbr)) continue;
                    double tentative_g = current.g + 1; // uniform cost
                    boolean better = false;
                    if (!openSet.contains(nbr)) {
                        openSet.add(nbr);
                        better = true;
                    } else if (tentative_g < nbr.g) {
                        better = true;
                    }
                    if (better) {
                        nbr.parent = current;
                        nbr.g = tentative_g;
                        nbr.f = nbr.g + heuristic(nbr, goalNode);
                        // update PQ: remove and re-add (no decrease-key)
                        openPQ.remove(nbr);
                        openPQ.add(nbr);
                    }
                }

                repaint();
                sleep(delayMs);
            }

            if (!found) {
                JOptionPane.showMessageDialog(this, "No path found.", "Result", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private void reconstructPath(Node end) {
            Node cur = end;
            finalPath.clear();
            while (cur != null && !cur.equals(startNode)) {
                finalPath.add(cur);
                cur = cur.parent;
            }
            // mark path cells
            for (Node p : finalPath) {
                if (!p.equals(goalNode)) grid[p.r][p.c] = CellType.PATH;
            }
            repaint();
        }

        private void sleep(int ms) {
            try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            for (int r=0;r<rows;r++) {
                for (int c=0;c<cols;c++) {
                    int x = c*cellSize, y = r*cellSize;
                    CellType t = grid[r][c];
                    switch (t) {
                        case EMPTY: g2.setColor(Color.WHITE); break;
                        case WALL: g2.setColor(Color.DARK_GRAY); break;
                        case START: g2.setColor(Color.GREEN); break;
                        case GOAL: g2.setColor(Color.RED); break;
                        case PATH: g2.setColor(Color.CYAN); break;
                    }
                    g2.fillRect(x, y, cellSize, cellSize);
                    // draw open/closed overlay
                    Node n = nodes[r][c];
                    if (closedSet.contains(n) && t==CellType.EMPTY) {
                        g2.setColor(new Color(255,200,200)); g2.fillRect(x, y, cellSize, cellSize);
                    } else if (openSet.contains(n) && t==CellType.EMPTY) {
                        g2.setColor(new Color(200,255,200)); g2.fillRect(x, y, cellSize, cellSize);
                    }
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawRect(x, y, cellSize, cellSize);
                }
            }
            // draw f/g values if small grid
            if (rows*cols <= 900) {
                g2.setColor(Color.BLACK);
                for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
                    Node n = nodes[r][c];
                    int x = c*cellSize, y = r*cellSize;
                    if (n.f < Double.POSITIVE_INFINITY && grid[r][c]==CellType.EMPTY) {
                        String s = String.format("%.0f", n.f);
                        g2.drawString(s, x+4, y+12);
                    }
                }
            }
        }
    }
}


