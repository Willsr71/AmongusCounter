package sr.will.amonguscounter.history;

import java.io.DataOutputStream;
import java.io.File;

public class IntermediateHistoryFile {
    public String date;
    public long timestamp;
    public File file;
    public DataOutputStream outputStream;

    public IntermediateHistoryFile(String date, long timestamp, File file, DataOutputStream outputStream) {
        this.date = date;
        this.timestamp = timestamp;
        this.file = file;
        this.outputStream = outputStream;
    }
}
