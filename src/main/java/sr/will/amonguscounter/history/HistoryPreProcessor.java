package sr.will.amonguscounter.history;

import sr.will.amonguscounter.App;
import sr.will.amonguscounter.Arguments;
import sr.will.amonguscounter.Main;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class HistoryPreProcessor {
    final Arguments arguments;

    Map<String, IntermediateHistoryFile> intermediateHistoryFiles = new HashMap<>();
    int[] colorsIndex = new int[App.numColors];
    int[] colorUses = new int[App.numColors];
    byte[] colorRemaps = new byte[App.numColors];

    public HistoryPreProcessor(Arguments arguments) {
        this.arguments = arguments;
    }

    public void run() throws IOException {
        Main.LOGGER.info("Pre-processing history...");
        long startTime = System.currentTimeMillis();

        new RawFileProcessor(this, new File(arguments.workingDirectory, arguments.historyFile)).run();

        remapColors();

        Main.LOGGER.info("Color remaps:");
        for (byte i = 0; i < App.numColors; i++) {
            Main.LOGGER.info("Color {} -> {}, {} usages ({})", colorRemaps[i], i, colorUses[colorRemaps[i]], colorsIndex[colorRemaps[i]]);
        }

        sortHistory();

        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("Finished pre-processing history, took {}ms", endTime - startTime);
    }

    private void remapColors() {
        boolean[] usedColors = new boolean[App.numColors];

        for (byte i = 0; i < App.numColors; i++) {
            byte originalColorIndex = getNextColorIndex(usedColors);
            usedColors[originalColorIndex] = true;
            colorRemaps[i] = originalColorIndex;
        }
    }

    private byte getNextColorIndex(boolean[] usedColors) {
        byte nextColorIndex = -1;
        for (byte i = 0; i < App.numColors; i++) {
            if (usedColors[i]) continue;
            if (nextColorIndex == -1 || colorUses[i] >= colorUses[nextColorIndex]) {
                nextColorIndex = i;
            }
        }
        return nextColorIndex;
    }

    private void sortHistory() throws IOException {
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(arguments.workingDirectory, arguments.processedHistoryFile))));
        for (IntermediateHistoryFile file : intermediateHistoryFiles.values()) {
            Main.LOGGER.info(file.date);
            new HistorySorter(file.file, outputStream).run();
        }
    }
}
