package sr.will.amonguscounter.history;

import sr.will.amonguscounter.Main;

import java.io.*;

public class ChunkMerger {
    private final HistoryPreProcessor parent;
    private final int numChunks;

    public ChunkMerger(HistoryPreProcessor parent) {
        this.parent = parent;
        this.numChunks = parent.chunkedFiles.size();
    }

    public void run() throws IOException {
        DataInputStream[] inputStreams = new DataInputStream[numChunks];
        long[] nextTimestamps = new long[numChunks];
        // We're using a long as a boolean bitflag, this makes it easy to check if we're done
        long status = (long) Math.pow(2, numChunks) - 1;

        Main.LOGGER.info("Merging {} chunks, initial status: {}...", numChunks, status);

        // Read initial data
        for (int i = 0; i < numChunks; i++) {
            inputStreams[i] = new DataInputStream(new BufferedInputStream(new FileInputStream(parent.chunkedFiles.get(i))));
            nextTimestamps[i] = inputStreams[i].readLong();
        }

        // Open output stream
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(parent.arguments.processedHistoryFile)));

        while (status != 0) {
            byte i = getNextEntryIndex(nextTimestamps);
            byte colorIndex = inputStreams[i].readByte();

            // Write data to file
            outputStream.writeLong(nextTimestamps[i]);
            outputStream.writeByte(parent.colorRemaps[colorIndex]); // Remap color to new index
            outputStream.write(inputStreams[i].readNBytes(8));

            if (inputStreams[i].available() == 0) {
                nextTimestamps[i] = 0;
                inputStreams[i].close();
                inputStreams[i] = null;
                status -= 1L << i;

                Main.LOGGER.info("Finished reading from file {}, status: {}, next timestamps: {}", i, status, nextTimestamps);
                continue;
            }

            nextTimestamps[i] = inputStreams[i].readLong();
        }

        // Clean up
        outputStream.close();
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
