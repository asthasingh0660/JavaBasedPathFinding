/*
 6️⃣ MainFrame.java (Control + Integration)

This is the main entry point:

Builds the window (JFrame)

Adds the GridPanel and control buttons

Handles “Start”, “Reset”, “Generate Maze”, etc.
 */


package src.ui;

import src.core.Grid;

import javax.swing.*;
import java.awt.*;

/**
 * Main application frame. Contains GridPanel and ControlPanel.
 * Also has main() to run the app.
 */
public class MainFrame extends JFrame {

    public MainFrame(int rows, int cols, int cellSize) {
        super("Pathfinding Visualizer - Modular");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Grid grid = new Grid(rows, cols);
        GridPanel gridPanel = new GridPanel(grid, cellSize);
        ControlPanel controlPanel = new ControlPanel(gridPanel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(gridPanel), BorderLayout.CENTER);
        getContentPane().add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mf = new MainFrame(31, 41, 20); // rows, cols, cellSize
            mf.setVisible(true);
        });
    }
}

