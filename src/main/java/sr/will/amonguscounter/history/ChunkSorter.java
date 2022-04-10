package sr.will.amonguscounter.history;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkSorter {

    public static void sortChunk(byte[] chunk, File file) throws IOException {
        Map<Long, int[]> timestampMap = getTimestampMap(chunk);

        List<Long> sortedTimestamps = timestampMap.keySet().stream().sorted().toList();
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        for (long timestamp : sortedTimestamps) {
            for (int pointer : timestampMap.get(timestamp)) {
                outputStream.write(chunk, pointer, 17);
            }
        }
        outputStream.close();
    }

    private static Map<Long, int[]> getTimestampMap(byte[] chunk) throws IOException {
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(chunk));
        Map<Long, int[]> timestampMap = new HashMap<>();
        for (int pointer = 0; pointer < chunk.length; pointer += 17) {
            long timestamp = inputStream.readLong();
            int[] pointerArr;
            if (timestampMap.containsKey(timestamp)) {
                int[] oldArr = timestampMap.get(timestamp);
                pointerArr = new int[oldArr.length + 1];
                System.arraycopy(oldArr, 0, pointerArr, 1, oldArr.length);
            } else {
                pointerArr = new int[1];
            }

            pointerArr[0] = pointer;
            timestampMap.put(timestamp, pointerArr);
            inputStream.skipNBytes(9); // 1 byte, 4 shorts
        }

        return timestampMap;
    }
}
