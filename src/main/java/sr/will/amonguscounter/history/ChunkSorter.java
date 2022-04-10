package sr.will.amonguscounter.history;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkSorter {

    public static void sortChunk(byte[] chunk, File file) throws IOException {
        Map<Long, Integer> timestampMap = getTimestampMap(chunk);

        List<Long> sortedTimestamps = timestampMap.keySet().stream().sorted().toList();
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        for (long timestamp : sortedTimestamps) {
            int pointer = timestampMap.remove(timestamp);
            outputStream.write(chunk, pointer, 17);
        }
        outputStream.close();
    }

    private static Map<Long, Integer> getTimestampMap(byte[] chunk) throws IOException {
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(chunk));
        Map<Long, Integer> timestampMap = new HashMap<>();
        for (int pointer = 0; pointer < chunk.length; pointer += 17) {
            long timestamp = inputStream.readLong();
            timestampMap.put(timestamp, pointer);
            inputStream.skipNBytes(9); // 1 byte, 4 shorts
        }

        return timestampMap;
    }
}
