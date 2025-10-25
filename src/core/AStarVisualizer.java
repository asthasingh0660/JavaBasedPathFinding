package src.core;

import src.ui.GridPanel;
import src.ui.ControlPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Compatibility wrapper so older references to core.AStarVisualizer still work.
 * This constructs a core.Grid and the new UI GridPanel + ControlPanel.
 */
public class AStarVisualizer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int rows = 31;
            int cols = 41;
            int cellSize = 20;

            Grid grid = new Grid(rows, cols);
            GridPanel gridPanel = new GridPanel(grid, cellSize);
            ControlPanel controlPanel = new ControlPanel(gridPanel);

            JFrame frame = new JFrame("A* Algorithm Visualizer - Modular (wrapper)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(new JScrollPane(gridPanel), BorderLayout.CENTER);
            frame.getContentPane().add(controlPanel, BorderLayout.SOUTH);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

 

