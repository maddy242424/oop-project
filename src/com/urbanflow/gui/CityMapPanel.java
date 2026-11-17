package com.urbanflow.gui;

import com.urbanflow.core.CityController;
import com.urbanflow.lighting.LightMode;
import com.urbanflow.lighting.StreetLight;
import com.urbanflow.model.VehicleType;
import com.urbanflow.parking.ParkingLot;
import com.urbanflow.parking.SlotType;
import com.urbanflow.traffic.Direction;
import com.urbanflow.traffic.Intersection;
import com.urbanflow.traffic.TrafficSubsystem;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Draws the live city map. It only READS from the simulation (through synchronized
 * getters that return copies) and never changes it, so drawing cannot corrupt data.
 * The dashboard calls repaint() several times a second; Swing then calls paintComponent().
 */
public class CityMapPanel extends JPanel {
    // layout (pixels)
    private static final int WIDTH = 940, HEIGHT = 440;
    private static final int ROAD_TOP = 190, ROAD_H = 40;           // main east-west road
    private static final int SIDE_TOP = 70, SIDE_BOTTOM = 350;      // north-south side roads
    private static final int[] INTERSECTION_X = {160, 470, 780};
    private static final int[] LIGHT_X = {40, 95, 250, 360, 410, 560, 680, 720, 890};
    private static final int LIGHT_Y = 165;
    private static final int CAR = 10;                               // size of one queued car

    private final CityController city;

    public CityMapPanel(CityController city) {
        this.city = city;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean night = city.getClock().isNight();

        drawBackground(g, night);
        drawRoads(g);
        drawLights(g);
        ArrayList<Intersection> intersections = city.getTraffic().getIntersections();
        for (int i = 0; i < intersections.size(); i++) {
            drawIntersection(g, intersections.get(i), INTERSECTION_X[i], night);
        }
        drawAmbulancesOnRoad(g);
        ArrayList<ParkingLot> lots = city.getParking().getLots();
        drawLot(g, lots.get(0), 195, 250, night);
        drawLot(g, lots.get(1), 812, 250, night);
        drawLegend(g, night);
    }

    // ---------------------------------------------------------------- background & roads
    private void drawBackground(Graphics2D g, boolean night) {
        g.setColor(night ? new Color(24, 30, 48) : new Color(222, 232, 214));
        g.fillRect(0, 0, getWidth(), getHeight());

        // area separators and names
        g.setColor(night ? new Color(60, 70, 95) : new Color(190, 200, 185));
        g.drawLine(313, 0, 313, HEIGHT);
        g.drawLine(627, 0, 627, HEIGHT);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(night ? new Color(170, 180, 210) : new Color(70, 80, 70));
        g.drawString("WEST", 20, 22);
        g.drawString("CENTRAL", 330, 22);
        g.drawString("EAST", 645, 22);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String header = city.getClock().timeString() + (night ? "  NIGHT" : "  DAY")
                + "   Signals: " + city.getTraffic().getSignalMode();
        g.drawString(header, 560, 50);
    }

    private void drawRoads(Graphics2D g) {
        g.setColor(new Color(85, 85, 90));
        g.fillRect(10, ROAD_TOP, WIDTH - 20, ROAD_H);
        for (int x : INTERSECTION_X) {
            g.fillRect(x - 20, SIDE_TOP, 40, SIDE_BOTTOM - SIDE_TOP);
        }
        // dashed centre line
        g.setColor(new Color(230, 230, 230));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {8, 8}, 0));
        g.drawLine(10, ROAD_TOP + ROAD_H / 2, WIDTH - 10, ROAD_TOP + ROAD_H / 2);
        g.setStroke(new BasicStroke(1));
    }

    // ---------------------------------------------------------------- street lights
    private void drawLights(Graphics2D g) {
        ArrayList<StreetLight> lights = city.getLighting().getLights();
        boolean flashOn = (System.currentTimeMillis() / 400) % 2 == 0;
        for (int i = 0; i < lights.size() && i < LIGHT_X.length; i++) {
            LightMode mode = lights.get(i).getMode();
            int x = LIGHT_X[i], y = LIGHT_Y;

            g.setColor(new Color(120, 120, 120));
            g.fillRect(x - 1, y, 3, ROAD_TOP - y);                    // pole

            Color core;
            int glow = 0;
            if (mode == LightMode.FAULTY) {
                core = new Color(200, 40, 40);
            } else if (mode == LightMode.FLASHING) {
                core = flashOn ? new Color(255, 140, 0) : new Color(90, 60, 20);
                glow = flashOn ? 16 : 0;
            } else if (mode == LightMode.FULL) {
                core = new Color(255, 245, 160);
                glow = 22;
            } else if (mode == LightMode.DIM) {
                core = new Color(220, 200, 110);
                glow = 11;
            } else {
                core = new Color(70, 70, 70);
            }
            if (glow > 0) {
                g.setColor(new Color(core.getRed(), core.getGreen(), core.getBlue(), 70));
                g.fillOval(x - glow, y - glow, glow * 2, glow * 2);
            }
            g.setColor(core);
            g.fillOval(x - 5, y - 5, 10, 10);
            if (mode == LightMode.FAULTY) {                         // red X on a broken light
                g.setStroke(new BasicStroke(2));
                g.drawLine(x - 8, y - 8, x + 8, y + 8);
                g.drawLine(x - 8, y + 8, x + 8, y - 8);
                g.setStroke(new BasicStroke(1));
            }
        }
    }

    // ---------------------------------------------------------------- intersections
    private void drawIntersection(Graphics2D g, Intersection in, int x, boolean night) {
        boolean blocked = in.isBlocked();
        boolean preempted = in.isPreempted();
        Direction green = in.getGreen();
        HashMap<Direction, Integer> lengths = in.getQueueLengths();
        HashMap<Direction, ArrayList<VehicleType>> queues = in.getQueueSnapshot(8);

        // stop lines: green for the approach that has green, red otherwise
        for (Direction d : Direction.values()) {
            boolean isGreen = (d == green) && (!blocked || preempted);
            g.setColor(isGreen ? new Color(40, 220, 90) : new Color(230, 50, 50));
            if (d == Direction.WEST)  g.fillRect(x - 25, ROAD_TOP + 20, 5, 20);
            if (d == Direction.EAST)  g.fillRect(x + 20, ROAD_TOP, 5, 20);
            if (d == Direction.NORTH) g.fillRect(x - 20, ROAD_TOP - 5, 20, 5);
            if (d == Direction.SOUTH) g.fillRect(x, ROAD_TOP + ROAD_H, 20, 5);
        }

        // queued vehicles, drawn as small squares lined up behind each stop line
        for (Direction d : Direction.values()) {
            ArrayList<VehicleType> list = queues.get(d);
            for (int k = 0; k < list.size(); k++) {
                int step = k * (CAR + 2);
                int cx, cy;
                if (d == Direction.WEST)       { cx = x - 27 - CAR - step; cy = ROAD_TOP + 25; }
                else if (d == Direction.EAST)  { cx = x + 27 + step;       cy = ROAD_TOP + 5; }
                else if (d == Direction.NORTH) { cx = x - 15;              cy = ROAD_TOP - 7 - CAR - step; }
                else                           { cx = x + 5;               cy = ROAD_TOP + ROAD_H + 7 + step; }
                drawVehicle(g, list.get(k), cx, cy);
            }
            int extra = lengths.get(d) - list.size();
            if (extra > 0) {
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                if (d == Direction.WEST)  g.drawString("+" + extra, x - 140, ROAD_TOP + 34);
                if (d == Direction.EAST)  g.drawString("+" + extra, x + 125, ROAD_TOP + 14);
                if (d == Direction.NORTH) g.drawString("+" + extra, x - 40, SIDE_TOP + 12);
                if (d == Direction.SOUTH) g.drawString("+" + extra, x + 24, SIDE_BOTTOM - 4);
            }
        }

        // labels
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(night ? Color.WHITE : Color.BLACK);
        g.drawString(in.getId(), x + 24, SIDE_TOP + 14);
        if (preempted) {
            g.setColor(new Color(40, 220, 90));
            g.drawString("GREEN WAVE", x - 36, SIDE_TOP - 6);
        }
        if (blocked) {
            g.setColor(new Color(255, 120, 0));
            g.setStroke(new BasicStroke(4));
            g.drawLine(x - 16, ROAD_TOP + 4, x + 16, ROAD_TOP + ROAD_H - 4);
            g.drawLine(x - 16, ROAD_TOP + ROAD_H - 4, x + 16, ROAD_TOP + 4);
            g.setStroke(new BasicStroke(1));
            g.drawString("ACCIDENT", x - 28, SIDE_TOP - 22);
        }
    }

    /** Ambulances driving between two junctions, placed by how far they have travelled. */
    private void drawAmbulancesOnRoad(Graphics2D g) {
        int total = TrafficSubsystem.AMBULANCE_TRAVEL_TICKS;
        for (int[] amb : city.getTraffic().getAmbulancesOnRoad()) {
            int target = amb[0], ticksLeft = amb[1];
            int fromX = (target == 0) ? 15 : INTERSECTION_X[target - 1] + 25;
            int toX = INTERSECTION_X[target] - 27 - CAR;
            double progress = 1.0 - Math.max(0, ticksLeft) / (double) total;
            int x = (int) (fromX + (toX - fromX) * progress);
            int y = ROAD_TOP + 23;
            g.setColor(new Color(230, 40, 40));
            g.fillRect(x, y, 16, 14);
            g.setColor(Color.WHITE);
            g.fillRect(x + 7, y + 2, 2, 10);
            g.fillRect(x + 3, y + 6, 10, 2);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString("AMB", x - 2, y - 2);
        }
    }

    private void drawVehicle(Graphics2D g, VehicleType type, int x, int y) {
        g.setColor(colorFor(type));
        g.fillRect(x, y, CAR, CAR);
        if (type == VehicleType.AMBULANCE) {                    // white cross on ambulances
            g.setColor(Color.WHITE);
            g.fillRect(x + 4, y + 1, 2, CAR - 2);
            g.fillRect(x + 1, y + 4, CAR - 2, 2);
        }
    }

    static Color colorFor(VehicleType type) {
        switch (type) {
            case CAR:       return new Color(59, 130, 246);
            case BIKE:      return new Color(34, 197, 94);
            case EV:        return new Color(20, 200, 200);
            case TRUCK:     return new Color(200, 120, 30);
            case AMBULANCE: return new Color(230, 40, 40);
            default:        return Color.GRAY;
        }
    }

    // ---------------------------------------------------------------- parking lots
    private void drawLot(Graphics2D g, ParkingLot lot, int x, int y, boolean night) {
        SlotType[] types = lot.getSlotTypes();
        VehicleType[] occupants = lot.getOccupantTypes();
        int cols = 4, size = 22, gap = 4;
        int rows = (types.length + cols - 1) / cols;
        int w = cols * (size + gap) + gap;
        int h = rows * (size + gap) + gap;

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(night ? Color.WHITE : Color.BLACK);
        g.drawString(lot.getName(), x, y - 4);

        g.setColor(night ? new Color(45, 50, 65) : new Color(245, 245, 240));
        g.fillRect(x, y, w, h);
        g.setColor(Color.GRAY);
        g.drawRect(x, y, w, h);

        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        for (int i = 0; i < types.length; i++) {
            int sx = x + gap + (i % cols) * (size + gap);
            int sy = y + gap + (i / cols) * (size + gap);
            if (occupants[i] != null) {
                g.setColor(colorFor(occupants[i]));
                g.fillRect(sx, sy, size, size);
            }
            g.setColor(types[i] == SlotType.EMERGENCY ? new Color(230, 40, 40) : Color.GRAY);
            g.drawRect(sx, sy, size, size);
            g.setColor(night ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g.drawString(slotLetter(types[i]), sx + 2, sy + 9);
        }

        // info under the lot
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(night ? Color.WHITE : Color.BLACK);
        boolean peak = city.getClock().isPeakHour();
        g.drawString(String.format("%.0f%% full", lot.getOccupancy() * 100), x, y + h + 14);
        g.drawString(String.format("Car Rs.%.0f/h", lot.currentPrice(SlotType.STANDARD, peak)), x, y + h + 27);
        int waiting = lot.queueLength();
        g.setColor(waiting >= 4 ? new Color(230, 50, 50) : (night ? Color.WHITE : Color.BLACK));
        g.drawString("Waiting: " + waiting, x, y + h + 40);

        if (lot.isClosed()) {
            g.setColor(new Color(255, 120, 0, 170));
            g.fillRect(x, y, w, h);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("CLOSED", x + 22, y + h / 2 + 5);
        }
    }

    private String slotLetter(SlotType t) {
        switch (t) {
            case BIKE:        return "B";
            case STANDARD:    return "S";
            case EV_CHARGING: return "EV";
            case HEAVY:       return "H";
            default:          return "+";
        }
    }

    // ---------------------------------------------------------------- legend
    private void drawLegend(Graphics2D g, boolean night) {
        int x = 330, y = 262;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        VehicleType[] types = {VehicleType.CAR, VehicleType.BIKE, VehicleType.EV, VehicleType.TRUCK, VehicleType.AMBULANCE};
        for (int i = 0; i < types.length; i++) {
            drawVehicle(g, types[i], x, y + i * 16);
            g.setColor(night ? Color.WHITE : Color.BLACK);
            g.drawString(types[i].name(), x + 16, y + i * 16 + 9);
        }
        g.setColor(night ? Color.LIGHT_GRAY : Color.DARK_GRAY);
        g.drawString("Light: dim / bright /", x, y + 92);
        g.drawString("flashing / red X = broken", x, y + 106);
    }
}
