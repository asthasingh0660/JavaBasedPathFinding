package src.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Small panel containing common controls wired to a GridPanel.
 */
public class ControlPanel extends JPanel {
    public ControlPanel(GridPanel gridPanel) {
        setLayout(new FlowLayout(FlowLayout.LEFT));

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
        randomBtn.addActionListener(e -> gridPanel.randomWalls(0.33));
        mazeBtn.addActionListener(e -> gridPanel.generateMaze());

        add(runBtn);
        add(clearPathBtn);
        add(resetBtn);
        add(randomBtn);
        add(mazeBtn);
        add(new JLabel("Delay"));
        add(speed);
    }
}

