package sr.will.amonguscounter.history;

import sr.will.amonguscounter.Main;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistorySorter {
    private final HistoryPreProcessor parent;
    private final int numChunks;

    public HistorySorter(HistoryPreProcessor parent) {
        this.parent = parent;
        this.numChunks = parent.chunkedFiles.size();
    }

    public void run() throws IOException {
        for (File file : parent.chunkedFiles) {
            Main.LOGGER.info("file: {}", file.getName());
            sortChunk(file);
        }

        mergeChunks();
    }

    public void mergeChunks() throws IOException {
        Main.LOGGER.info("Merging {} chunks...", numChunks);

        DataInputStream[] inputStreams = new DataInputStream[numChunks];
        long[] nextTimestamps = new long[numChunks];
        // We're using a long as a boolean bitflag, this makes it easy to check if we're done
        long status = (long) Math.pow(2, numChunks) - 1;

        // Read initial data
        for (int i = 0; i < numChunks; i++) {
            inputStreams[i] = new DataInputStream(new BufferedInputStream(new FileInputStream(parent.chunkedFiles.get(i))));
            nextTimestamps[i] = inputStreams[i].readLong();

            Main.LOGGER.info("First timestamp for file {}: {}", parent.chunkedFiles.get(i).getName(), nextTimestamps[i]);
        }

        // Open output stream
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(parent.arguments.processedHistoryFile)));

        while (status != 0) {
            byte i = getNextEntryIndex(nextTimestamps);

            // Write data to file
            outputStream.writeLong(nextTimestamps[i]);
            outputStream.write(inputStreams[i].readNBytes(9));

            if (inputStreams[i].available() == 0) {
                nextTimestamps[i] = 0;
                inputStreams[i].close();
                inputStreams[i] = null;
                status -= 1L << i;

                Main.LOGGER.info("Finished reading from file {}, status: {}", i, status);
                Main.LOGGER.info("Next timestamps: {}", nextTimestamps);
                continue;
            }

            nextTimestamps[i] = inputStreams[i].readLong();
        }

        // Clean up
        outputStream.close();
    }

    private void sortChunk(File file) throws IOException {
        byte[] chunk = readFileToBytes(file);
        Map<Long, Integer> timestampMap = getTimestampMap(chunk);

        List<Long> sortedTimestamps = timestampMap.keySet().stream().sorted().toList();
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        for (long timestamp : sortedTimestamps) {
            int pointer = timestampMap.remove(timestamp);
            outputStream.write(chunk, pointer, 17);
        }
        outputStream.close();
    }

    private Map<Long, Integer> getTimestampMap(byte[] chunk) throws IOException {
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(chunk));
        Map<Long, Integer> timestampMap = new HashMap<>();
        for (int pointer = 0; pointer < inputStream.available(); pointer += 17) {
            long timestamp = inputStream.readLong();
            timestampMap.put(timestamp, pointer);
            inputStream.skipBytes(9); // 1 byte, 4 shorts
        }

        return timestampMap;
    }

    private byte[] readFileToBytes(File file) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = inputStream.readAllBytes();
        inputStream.close();
        return bytes;
    }

    private byte getNextEntryIndex(long[] nextTimestamps) {
        byte nextEntryIndex = -1;
        for (byte i = 0; i < numChunks; i++) {
            if (nextTimestamps[i] == 0) continue;
            if (nextEntryIndex == -1 || nextTimestamps[i] < nextTimestamps[nextEntryIndex]) {
                nextEntryIndex = i;
            }
        }
        return nextEntryIndex;
    }
}
