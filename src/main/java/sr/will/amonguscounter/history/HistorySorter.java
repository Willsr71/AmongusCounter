package sr.will.amonguscounter.history;

import java.io.DataOutputStream;
import java.io.File;

public class HistorySorter {

    private final File inputFile;
    private final DataOutputStream outputStream;

    public HistorySorter(File inputFile, DataOutputStream outputStream) {
        this.inputFile = inputFile;
        this.outputStream = outputStream;
    }

    public void run() {

    }
}
