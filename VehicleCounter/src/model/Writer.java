package model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public final class Writer {
    private static String file = "vehicle_counter_data.csv";
    private static BufferedWriter writer;
    private static boolean open;

    public static void write(Vehicle vehicle) {
        try {
            if (!open) {
                open = true;
                writer = new BufferedWriter(new FileWriter(file));
                writer.write("id,seen,speed,direction,\n");
            }
            String output = vehicle.getId() + "," + vehicle.getLastSeen() + "," + vehicle.getSpeed() + ","
                    + vehicle.getDirection() + ",\n";
            writer.write(output);
        } catch (IOException e) {
            e.getMessage();
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.getMessage();
            e.printStackTrace();
        }
    }

    public static boolean isOpen() {
        return open;
    }
    
    public static void setPath(String file) {
        Writer.file = file;
    }
}
