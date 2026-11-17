package com.urbanflow.gui;

import com.urbanflow.core.CityController;
import com.urbanflow.events.CityEvent;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Main window:
 *   centre : live city map (CityMapPanel)
 *   right  : status text of every subsystem
 *   bottom : operator buttons + event log
 *
 * A javax.swing.Timer refreshes the screen 5 times a second. Swing timers run on the
 * Swing (event dispatch) thread, which is the only thread allowed to touch Swing components.
 */
public class CityDashboard extends JFrame {
    private final CityController city;
    private final CityMapPanel map;
    private final JTextArea statusArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JButton modeButton = new JButton();

    public CityDashboard(CityController city, String screenshotPath, int screenshotDelaySeconds) {
        super("UrbanFlow - Smart City Traffic, Lighting & Parking");
        this.city = city;
        this.map = new CityMapPanel(city);

        setLayout(new BorderLayout());
        add(map, BorderLayout.CENTER);
        add(buildStatusPanel(), BorderLayout.EAST);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        // refresh 5 times a second
        Timer refresh = new Timer(200, e -> refresh());
        refresh.start();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println(city.endOfDayReport());
                city.shutdown();
                dispose();
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        if (screenshotPath != null) {
            scheduleScreenshot(screenshotPath, screenshotDelaySeconds);
        }
    }

    private JPanel buildStatusPanel() {
        statusArea.setEditable(false);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(statusArea);
        scroll.setPreferredSize(new Dimension(400, 440));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("City status"));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JButton ambulanceButton = new JButton("Dispatch Ambulance");
        JComboBox<String> areaBox = new JComboBox<>(CityController.AREAS);
        areaBox.setSelectedItem("Central");
        JButton accidentButton = new JButton("Report Accident");
        JButton clearButton = new JButton("Clear Accident");
        JButton reportButton = new JButton("End-of-Day Report");
        modeButton.setText("Switch to Fixed Signals");

        // button actions (lambdas: short for "new ActionListener() { actionPerformed(...) }")
        ambulanceButton.addActionListener(e -> city.dispatchAmbulance());
        accidentButton.addActionListener(e -> city.reportAccident((String) areaBox.getSelectedItem()));
        clearButton.addActionListener(e -> city.clearAccident((String) areaBox.getSelectedItem()));
        modeButton.addActionListener(e -> {
            String mode = city.toggleSignalMode();
            modeButton.setText(mode.equals("Adaptive") ? "Switch to Fixed Signals" : "Switch to Adaptive Signals");
        });
        reportButton.addActionListener(e -> showReport());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(ambulanceButton);
        buttons.add(new JLabel("   Area:"));
        buttons.add(areaBox);
        buttons.add(accidentButton);
        buttons.add(clearButton);
        buttons.add(modeButton);
        buttons.add(reportButton);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setRows(8);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Event log (newest first)"));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(logScroll, BorderLayout.CENTER);
        return bottom;
    }

    private void refresh() {
        map.repaint();
        statusArea.setText(city.statusText());
        StringBuilder sb = new StringBuilder();
        for (CityEvent e : city.getBus().getRecentEvents(40)) {
            sb.append(e).append('\n');
        }
        logArea.setText(sb.toString());
        logArea.setCaretPosition(0);
    }

    private void showReport() {
        JTextArea text = new JTextArea(city.endOfDayReport());
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        text.setEditable(false);
        JOptionPane.showMessageDialog(this, text, "End-of-Day Report", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Testing helper: save a picture of the window after a delay, then exit. */
    private void scheduleScreenshot(String path, int delaySeconds) {
        // trigger an accident and an ambulance shortly before the picture, so both are visible
        Timer accident = new Timer(Math.max(1, delaySeconds - 6) * 1000, e -> city.reportAccident("Central"));
        accident.setRepeats(false);
        accident.start();
        Timer ambulance = new Timer(Math.max(1, delaySeconds - 3) * 1000, e -> city.dispatchAmbulance());
        ambulance.setRepeats(false);
        ambulance.start();

        Timer shot = new Timer(delaySeconds * 1000, e -> {
            refresh();
            BufferedImage img = new BufferedImage(getContentPane().getWidth(),
                    getContentPane().getHeight(), BufferedImage.TYPE_INT_RGB);
            getContentPane().paint(img.getGraphics());
            try {
                ImageIO.write(img, "png", new File(path));
                System.out.println("Screenshot saved to " + path);
            } catch (IOException ex) {
                System.err.println("Could not save screenshot: " + ex.getMessage());
            }
            System.out.println(city.endOfDayReport());
            city.shutdown();
            System.exit(0);
        });
        shot.setRepeats(false);
        shot.start();
    }
}
