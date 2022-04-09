package sr.will.amonguscounter.history;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import sr.will.amonguscounter.App;
import sr.will.amonguscounter.Main;

import java.io.*;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class HistoryPreProcessor {
    public static Map<String, Long> dateTimestamps = new HashMap<>();

    private final File input;
    private final File output;
    private int[] colorsIndex = new int[32];
    private int[] colorUses = new int[32];
    private byte usedColorIndexes = 0;

    public HistoryPreProcessor(File input, File output) {
        this.input = input;
        this.output = output;
    }

    public void run() throws IOException {
        // TODO the CSV is not sorted by date
        Main.LOGGER.info("Pre-processing history...");
        long startTime = System.currentTimeMillis();
        int lines = 0;

        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.selectFields("timestamp", "pixel_color", "coordinate");
        parserSettings.setHeaders("timestamp", "user_id", "pixel_color", "coordinate");
        parserSettings.setHeaderExtractionEnabled(true);
        CsvParser parser = new CsvParser(parserSettings);

        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

        for (String[] row : parser.iterate(input)) {
            processRow(row, outputStream);

            lines++;
            if (!App.running) break;
        }

        outputStream.close();

        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("Finished pre-processing {} lines, took {}ms ({}ms/line)", lines, endTime - startTime, (double) (endTime - startTime) / (double) lines);
        Main.LOGGER.info("Color uses: {}", colorUses);
    }

    private void processRow(String[] row, DataOutputStream outputStream) throws IOException {
        outputStream.writeLong(parseTime(row[0]));
        byte colorIndex = getColorIndex(Integer.parseInt(row[1].substring(1), 16));
        colorUses[colorIndex]++;
        outputStream.writeByte(colorIndex);

        for (short coord : getCoords(row[2])) {
            outputStream.writeShort(coord);
        }
        /*
        String[] coordStrings = row[2].split(",");
        outputStream.writeShort(Short.parseShort(coordStrings[0]));
        outputStream.writeShort(Short.parseShort(coordStrings[1]));
        if (coordStrings.length == 2) {
            outputStream.writeBoolean(false);
        } else {
            outputStream.writeBoolean(true);
            outputStream.writeShort(Short.parseShort(coordStrings[2]));
            outputStream.writeShort(Short.parseShort(coordStrings[3]));
        }
         */
    }

    private short[] getCoords(String coordString) {
        byte[] coordBytes = coordString.getBytes();
        short[] coords = new short[4];

        for (int i = 0, currentStart = 0, coordIndex = 0; i < coordBytes.length; i++) {
            if (coordBytes[i] == ',' || i + 1 == coordBytes.length) {
                byte[] currentBytes = new byte[i - currentStart];
                System.arraycopy(coordBytes, currentStart == 0 ? 0 : currentStart + 1, currentBytes, 0, i - currentStart);
                coords[coordIndex] = (short) getNumber(currentBytes);

                coordIndex++;
                currentStart = i;
            }
        }

        return coords;
    }

    // Parsing the entire date and time every line os way too slow
    // This stores the date in a map and just calculates the time
    private long parseTime(String dateTimeString) {
        long time = 0;
        String dateStr = dateTimeString.substring(0, 10);
        String timeStr = dateTimeString.substring(11, dateTimeString.lastIndexOf(' '));

        if (dateTimestamps.containsKey(dateStr)) {
            time = dateTimestamps.get(dateStr);
        } else {
            time = ZonedDateTime.of(
                    Integer.parseInt(dateStr.substring(0, 4)),
                    Integer.parseInt(dateStr.substring(5, 7)),
                    Integer.parseInt(dateStr.substring(8, 10)),
                    0,
                    0,
                    0,
                    0,
                    ZoneId.ofOffset("UTC", ZoneOffset.UTC)
            ).toInstant().toEpochMilli();
            dateTimestamps.put(dateStr, time);
            Main.LOGGER.info(dateTimestamps.toString());
        }

        time = getTimeParsed(time, timeStr);
        //time = getTimeFromBytes(time, timeStr);
        return time;
    }

    private long getTimeParsed(long baseTime, String timeStr) {
        return baseTime + Integer.parseInt(timeStr.substring(0, 2)) * (1000L * 60 * 60) + // Hours
                Integer.parseInt(timeStr.substring(3, 5)) * (1000L * 60) + // Minutes
                (long) (Double.parseDouble(timeStr.substring(6)) * (1000L));// Seconds;
    }

    private long getTimeFromBytes(long baseTime, String timeStr) {
        byte[] timeBytes = timeStr.getBytes();
        return baseTime + getNumber(timeBytes[0], timeBytes[1]) * (1000L * 60 * 60) +
                getNumber(timeBytes[3], timeBytes[4]) * (1000L * 60) +
                getNumber(timeBytes[6], timeBytes[7]) * (1000L) +
                (long) (getDecimalNumber(timeStr.substring(9).getBytes()) * 1000L);
    }

    private int getNumber(byte... bytes) {
        int number = 0;
        for (int i = 0; i < bytes.length; i++) {
            number += (bytes[i] - 48) * Math.pow(10, bytes.length - i - 1);
        }
        return number;
    }

    private double getDecimalNumber(byte... bytes) {
        double number = 0;
        for (int i = 0; i < bytes.length; i++) {
            number += (bytes[i] - 48) * Math.pow(0.1, i + 1);
        }
        return number;
    }

    private byte getColorIndex(int color) {
        for (byte i = 0; i < usedColorIndexes; i++) {
            if (colorsIndex[i] == color) return i;
        }
        colorsIndex[usedColorIndexes] = color;
        usedColorIndexes++;
        Main.LOGGER.info("Color index: {}", colorsIndex);
        return (byte) (usedColorIndexes - 1);
    }
}
