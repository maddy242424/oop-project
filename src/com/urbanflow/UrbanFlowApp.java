package com.urbanflow;

import com.urbanflow.core.CityController;
import com.urbanflow.gui.CityDashboard;

import javax.swing.SwingUtilities;

/**
 * Entry point.
 *
 *   java -cp out com.urbanflow.UrbanFlowApp              -> opens the Swing dashboard
 *   java -cp out com.urbanflow.UrbanFlowApp --console 60 -> runs a scripted demo for 60 s
 *                                                          and prints reports (no window)
 */
public class UrbanFlowApp {

    public static void main(String[] args) throws InterruptedException {
        CityController city = CityController.getInstance();

        if (args.length > 0 && args[0].equals("--console")) {
            int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 60;
            runConsoleDemo(city, seconds);
            return;
        }

        city.start();
        String screenshot = args.length > 1 && args[0].equals("--screenshot") ? args[1] : null;
        int delay = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        SwingUtilities.invokeLater(() -> new CityDashboard(city, screenshot, delay));
    }

    /**
     * Scripted run used for testing and for showing the system without the GUI.
     * Every scenario from the problem statement is triggered at least once.
     */
    private static void runConsoleDemo(CityController city, int seconds) throws InterruptedException {
        city.getBus().setEchoToConsole(true);
        city.start();
        System.out.println("UrbanFlow console demo for " + seconds + " s\n");

        // The scenario repeats every 40 seconds, so long runs keep exercising everything.
        String[] areas = CityController.AREAS;
        int half = seconds / 2;
        for (int t = 1; t <= seconds; t++) {
            Thread.sleep(1000);
            int phase = t % 40;
            String area = areas[(t / 40) % areas.length];
            if (phase == 6) city.dispatchAmbulance();
            if (phase == 10) city.reportAccident(area);
            if (phase == 14) city.dispatchAmbulance();      // ambulance drives through the accident area
            if (phase == 22) city.clearAccident(area);
            if (t == half) city.toggleSignalMode();         // second half runs with fixed timers
            if (t % 30 == 0) System.out.println("\n" + city.statusText());
        }
        System.out.println(city.endOfDayReport());
        city.shutdown();
    }
}
