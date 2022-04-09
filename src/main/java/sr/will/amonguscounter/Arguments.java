package sr.will.amonguscounter;

import java.io.File;

public class Arguments {
    public short width = 2000;
    public short height = 2000;
    public boolean forcePreProcess = true;
    public File workingDirectory = new File("C:\\temp\\");
    public String historyFile = "2022_place_canvas_history.csv";
    public String processedHistoryFile = "history.bin";
    public String amongusFile = "amongus.csv";
    public byte allowedErrors = 0;

    public static File checkFile(String name) {
        File file = new File(name);
        if (!file.exists()) throw new RuntimeException("File does not exist!");
        return file;
    }

    public static String checkFileExists(String name) {
        return checkFile(name).getName();
    }
}
