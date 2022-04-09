package sr.will.amonguscounter;

import java.io.File;

public class Arguments {
    public short width = 2000;
    public short height = 2000;
    public boolean forcePreProcess = true;
    public File rawHistoryFile = new File("C:\\temp\\2022_place_canvas_history.csv");
    public File historyFile = new File("C:\\temp\\history.bin");
    public File amongusFile = new File("C:\\temp\\amongus.csv");
    public byte allowedErrors = 0;

    public static File checkFile(String name) {
        File file = new File(name);
        if (!file.exists()) throw new RuntimeException("File does not exist!");
        return file;
    }
}
