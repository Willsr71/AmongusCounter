package sr.will.amonguscounter;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    public static final Logger LOGGER = LoggerFactory.getLogger("Amongus");

    public static void main(String[] args) throws IOException {
        new App(getArguments(args));
    }

    public static Arguments getArguments(String[] args) {
        CommandLine cmd = parseArgs(args);
        Arguments arguments = new Arguments();

        if (cmd.hasOption("rawHistoryFile"))
            arguments.rawHistoryFile = Arguments.checkFile(cmd.getOptionValue("rawHistoryFile"));
        if (cmd.hasOption("historyFile"))
            arguments.historyFile = Arguments.checkFile(cmd.getOptionValue("historyFile"));
        if (cmd.hasOption("amongusFile"))
            arguments.amongusFile = Arguments.checkFile(cmd.getOptionValue("amongusFile"));
        if (cmd.hasOption("allowedErrors"))
            arguments.allowedErrors = Byte.parseByte(cmd.getOptionValue("allowedErrors"));

        return arguments;
    }

    public static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("rawHistoryFile")
                .desc("Raw history file")
                .hasArg().build());

        options.addOption(Option.builder("historyFile")
                .desc("History file")
                .hasArg().build());

        options.addOption(Option.builder("amongusFile")
                .desc("Amongus file")
                .hasArg().build());

        options.addOption(Option.builder("allowedErrors")
                .desc("Allowed errors per pattern")
                .hasArg().build());

        CommandLineParser parser = new DefaultParser();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            System.exit(1);
            return null;
        }
    }
}
