/*
A* Algorithm Visualizer - Java Swing (Enhanced)

What's new in this version:
- Proper non-blocking visualization using SwingWorker (so UI stays responsive)
- Exploration animation: cells being considered are shown in a yellow gradient
- Final path animation: after A* finds the goal, the path is animated in blue
- Maze generator (Recursive backtracker) accessible via "Generate Maze" button
- Buttons preserved: Run A*, Clear Path, Reset, Random Walls; added Generate Maze
- Keeps same single-file usage: save as AStarVisualizer.java, compile & run

How to use (same as before):
1. Save as AStarVisualizer.java
2. Compile: javac AStarVisualizer.java
3. Run: java AStarVisualizer

Controls:
- Left-click: toggle wall
- Right-click: set Start (first) and Goal (second), subsequent right-clicks move nearest of them
- SHIFT+drag with left-button: paint walls
- Generate Maze: create a perfect maze
- Run A*: visualizes exploration (yellow) and final path (blue)

This file is intentionally compact but structured so you can extend it further (weights, step-by-step mode, export GIF).
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Random;
import java.util.Arrays;
import java.util.Objects;

public class AStarVisualizer extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AStarVisualizer v = new AStarVisualizer(31, 41, 20); // rows, cols, cellSize (odd dims are good for mazes)
            v.setVisible(true);
        });
    }

    private GridPanel gridPanel;

    public AStarVisualizer(int rows, int cols, int cellSize) {
        super("A* Algorithm Visualizer - Java Swing (Enhanced)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gridPanel = new GridPanel(rows, cols, cellSize);

        JPanel control = new JPanel();
        JButton runBtn = new JButton("Run A*");
        JButton clearPathBtn = new JButton("Clear Path");
        JButton resetBtn = new JButton("Reset");
        JButton randomBtn = new JButton("Random Walls");
        JButton mazeBtn = new JButton("Generate Maze");
        JSlider speed = new JSlider(0, 200, 40);
        speed.setToolTipText("Visualization delay (ms)");

        runBtn.addActionListener(e -> gridPanel.runAStar(speed.getValue()));
        clearPathBtn.addActionListener(e -> gridPanel.clearPath());
        resetBtn.addActionListener(e -> gridPanel.resetAll());
        randomBtn.addActionListener(e -> gridPanel.randomWalls(0.3));
        mazeBtn.addActionListener(e -> gridPanel.generateMaze());

        control.add(runBtn);
        control.add(clearPathBtn);
        control.add(resetBtn);
        control.add(randomBtn);
        control.add(mazeBtn);
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
            int visitOrder = -1; // for gradient
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
        private Set<Node> closedSet = Collections.synchronizedSet(new HashSet<>());
        private Set<Node> openSet = Collections.synchronizedSet(new HashSet<>());
        private List<Node> finalPath = Collections.synchronizedList(new ArrayList<>());

        private volatile boolean painting = false;
        private volatile int visitCounter = 0;

        public GridPanel(int rows, int cols, int cellSize) {
            this.rows = rows; this.cols = cols; this.cellSize = cellSize;
            setPreferredSize(new Dimension(cols*cellSize, rows*cellSize));
            initGrid();
            setBackground(Color.WHITE);

            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    int r = e.getY()/cellSize;
                    int c = e.getX()/cellSize;
                    if (!inBounds(r, c)) return;
                    if (SwingUtilities.isRightMouseButton(e)) {
                        if (startNode == null) {
                            setCell(r, c, CellType.START);
                            startNode = nodes[r][c];
                        } else if (goalNode == null) {
                            setCell(r, c, CellType.GOAL);
                            goalNode = nodes[r][c];
                        } else {
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
                            painting = true;
                            toggleWall(r, c, true);
                        } else {
                            toggleWall(r, c, false);
                        }
                        clearSearchState();
                        repaint();
                    }
                }
                public void mouseReleased(MouseEvent e) { painting = false; }
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
            visitCounter = 0;
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
            return Math.abs(a.r - b.r) + Math.abs(a.c - b.c);
        }

        // Run A* in a background SwingWorker and publish visited nodes so paintComponent can animate them
        public void runAStar(int delayMs) {
            clearSearchState();
            if (startNode==null || goalNode==null) {
                JOptionPane.showMessageDialog(this, "Please set a Start and a Goal (right-click).", "Missing points", JOptionPane.WARNING_MESSAGE);
                return;
            }

            SwingWorker<Void, Node> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    PriorityQueue<Node> openPQ = new PriorityQueue<>();
                    startNode.g = 0; startNode.f = heuristic(startNode, goalNode);
                    openPQ.add(startNode);
                    openSet.add(startNode);

                    boolean found = false;

                    while (!openPQ.isEmpty()) {
                        Node current = openPQ.poll();
                        openSet.remove(current);
                        if (closedSet.contains(current)) continue; // may have duplicates
                        closedSet.add(current);

                        // mark visit order for gradient
                        current.visitOrder = visitCounter++;
                        publish(current); // will cause process() to run on EDT

                        if (current.equals(goalNode)) { found = true; reconstructPath(current); break; }

                        for (Node nbr : neighbors(current)) {
                            if (closedSet.contains(nbr)) continue;
                            double tentative_g = current.g + 1;
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
                                openPQ.remove(nbr);
                                openPQ.add(nbr);
                            }
                        }

                        try { Thread.sleep(Math.max(0, delayMs)); } catch (InterruptedException ignored) {}
                    }

                    if (!found) SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GridPanel.this, "No path found.", "Result", JOptionPane.INFORMATION_MESSAGE));
                    return null;
                }

                @Override
                protected void process(List<Node> chunks) {
                    // chunks are published visited nodes; repaint to show gradient
                    repaint();
                }
            };
            worker.execute();
        }

        private void reconstructPath(Node end) {
            Node cur = end;
            finalPath.clear();
            while (cur != null && !cur.equals(startNode)) {
                finalPath.add(cur);
                cur = cur.parent;
            }
            // animate path on EDT
            SwingUtilities.invokeLater(() -> animatePath());
        }

        private void animatePath() {
            // Show path cells one by one in blue
            Timer t = new Timer(50, null);
            final int[] idx = {finalPath.size()-1}; // start from start->goal order
            t.addActionListener(e -> {
                if (idx[0] < 0) { ((Timer)e.getSource()).stop(); return; }
                Node p = finalPath.get(idx[0]);
                if (!p.equals(goalNode) && !p.equals(startNode)) grid[p.r][p.c] = CellType.PATH;
                idx[0]--;
                repaint();
            });
            t.start();
        }

        // Maze generator (recursive backtracker). Leaves start/goal untouched if present.
        public void generateMaze() {
            // Make everything a wall
            for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) grid[r][c] = CellType.WALL;
            // Start from a random odd cell
            Random rnd = new Random();
            int sr = rnd.nextInt(rows/2)*2+1; // odd
            int sc = rnd.nextInt(cols/2)*2+1; // odd
            carve(sr, sc, rnd);
            if (startNode!=null) grid[startNode.r][startNode.c] = CellType.START;
            if (goalNode!=null) grid[goalNode.r][goalNode.c] = CellType.GOAL;
            clearSearchState();
            repaint();
        }

        private void carve(int r, int c, Random rnd) {
            grid[r][c] = CellType.EMPTY;
            Integer[] dirs = {0,1,2,3};
            List<Integer> order = Arrays.asList(dirs);
            Collections.shuffle(order, rnd);
            for (int d : order) {
                int dr = 0, dc = 0;
                if (d==0) { dr = -2; dc = 0; }
                if (d==1) { dr = 2; dc = 0; }
                if (d==2) { dr = 0; dc = -2; }
                if (d==3) { dr = 0; dc = 2; }
                int nr = r + dr, nc = c + dc;
                if (nr <= 0 || nr >= rows-1 || nc <= 0 || nc >= cols-1) continue;
                if (grid[nr][nc] == CellType.WALL) {
                    // knock down between
                    grid[r + dr/2][c + dc/2] = CellType.EMPTY;
                    carve(nr, nc, rnd);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int maxVisit = Math.max(1, visitCounter);
            for (int r=0;r<rows;r++) {
                for (int c=0;c<cols;c++) {
                    int x = c*cellSize, y = r*cellSize;
                    CellType t = grid[r][c];
                    switch (t) {
                        case EMPTY: g2.setColor(Color.WHITE); break;
                        case WALL: g2.setColor(Color.DARK_GRAY); break;
                        case START: g2.setColor(new Color(34,139,34)); break; // darker green
                        case GOAL: g2.setColor(new Color(178,34,34)); break; // darker red
                        case PATH: g2.setColor(new Color(30,144,255)); break; // DodgerBlue for final path
                        default: g2.setColor(Color.WHITE); break;
                    }
                    g2.fillRect(x, y, cellSize, cellSize);

                    // draw exploration overlay: if node has visitOrder, paint yellow-ish with gradient depending on order
                    Node n = nodes[r][c];
                    if (n.visitOrder >= 0 && grid[r][c]==CellType.EMPTY) {
                        float frac = (float)n.visitOrder / (float)maxVisit;
                        // earlier visits are brighter
                        int alpha = 80 + (int)((1.0 - frac) * 120); // 80..200
                        g2.setColor(new Color(255, 215, 0, Math.max(0, Math.min(255, alpha)))); // gold/yellow
                        g2.fillRect(x, y, cellSize, cellSize);
                    }

                    // open/closed tint for empties
                    if (closedSet.contains(n) && grid[r][c]==CellType.EMPTY) {
                        // subtle red tint already covered by visit overlay; optional extra border
                        g2.setColor(new Color(180,80,80,60)); g2.fillRect(x, y, cellSize, cellSize);
                    } else if (openSet.contains(n) && grid[r][c]==CellType.EMPTY) {
                        g2.setColor(new Color(80,180,80,50)); g2.fillRect(x, y, cellSize, cellSize);
                    }

                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawRect(x, y, cellSize, cellSize);
                }
            }

            // small grid: draw f values
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
