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

public class RawFileProcessor {
    private final HistoryPreProcessor parent;
    private final File input;
    private final Map<String, Long> baseTimestamps = new HashMap<>();

    private byte usedColorIndexes = 0;
    private int chunkNumber = -1;
    private int chunkEntries;
    private DataOutputStream outputStream;
    private ByteArrayOutputStream byteStream;

    public RawFileProcessor(HistoryPreProcessor parent, File input) {
        this.parent = parent;
        this.input = input;
    }

    public void run() throws IOException {
        Main.LOGGER.info("Processing raw history file...");
        long startTime = System.currentTimeMillis();

        // CSV Input
        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.selectFields("timestamp", "pixel_color", "coordinate");
        parserSettings.setHeaders("timestamp", "user_id", "pixel_color", "coordinate");
        parserSettings.setHeaderExtractionEnabled(true);
        CsvParser parser = new CsvParser(parserSettings);

        // Chunked output
        createNewChunk();

        int lines = 0;
        for (String[] row : parser.iterate(input)) {
            processRow(row);

            lines++;
            if (!App.running) break;
        }

        outputStream.close();
        sortChunk();

        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("Finished processing {} raw history lines, took {}ms ({}ns/line)", lines, endTime - startTime, ((double) (endTime - startTime) / (double) lines) * 1000d);
    }

    private void processRow(String[] row) throws IOException {
        String dateStr = row[0].substring(0, 10);
        String timeStr = row[0].substring(11, row[0].lastIndexOf(' '));

        // Parsing the entire date and time every line os way too slow
        // This stores the date in a map and just calculates the time
        outputStream.writeLong(getTime(dateStr, timeStr));

        byte colorIndex = getColorIndex(Integer.parseInt(row[1].substring(1), 16));
        parent.colorUses[colorIndex]++;
        outputStream.writeByte(colorIndex);

        for (short coord : getCoords(row[2])) {
            outputStream.writeShort(coord);
        }

        chunkEntries++;
        if (chunkEntries == parent.arguments.maxChunkEntries) {
            outputStream.close();
            sortChunk();
            createNewChunk();
        }
    }

    private void createNewChunk() {
        chunkEntries = 0;
        chunkNumber++;
        byteStream = new ByteArrayOutputStream();
        outputStream = new DataOutputStream(byteStream);
    }

    private void sortChunk() throws IOException {
        File chunkFile = new File(parent.arguments.chunkedDirectory, "chunk_" + chunkNumber + ".bin");
        ChunkSorter.sortChunk(byteStream.toByteArray(), chunkFile);
        parent.chunkedFiles.add(chunkFile);
        Main.LOGGER.info("Finished writing chunk {} with {} entries, size: {}", chunkNumber, chunkEntries, byteStream.size());
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

    private long getTime(String dateStr, String timeStr) {
        if (baseTimestamps.containsKey(dateStr)) return getTimeParsed(baseTimestamps.get(dateStr), timeStr);

        long timestamp = ZonedDateTime.of(
                Integer.parseInt(dateStr.substring(0, 4)),
                Integer.parseInt(dateStr.substring(5, 7)),
                Integer.parseInt(dateStr.substring(8, 10)),
                0,
                0,
                0,
                0,
                ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        ).toInstant().toEpochMilli();

        baseTimestamps.put(dateStr, timestamp);
        return getTimeParsed(timestamp, timeStr);
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
            if (parent.colorsIndex[i] == color) return i;
        }
        parent.colorsIndex[usedColorIndexes] = color;
        usedColorIndexes++;
        return (byte) (usedColorIndexes - 1);
    }
}
