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

        if (cmd.hasOption("patterns")) arguments.setPatternsFolder(cmd.getOptionValue("patterns"));
        if (cmd.hasOption("image")) arguments.setImage(cmd.getOptionValue("image"));
        if (cmd.hasOption("outputImage")) arguments.outputImage = cmd.getOptionValue("outputImage");
        if (cmd.hasOption("allowedErrors"))
            arguments.allowedErrors = Byte.parseByte(cmd.getOptionValue("allowedErrors"));

        return arguments;
    }

    public static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("patterns")
                .desc("Pattern folder")
                .hasArg().build());

        options.addOption(Option.builder("image")
                .desc("Image file")
                .hasArg().build());

        options.addOption(Option.builder("outputImage")
                .desc("Output image file")
                .hasArg().build());

        options.addOption(Option.builder("allowedErrors")
                .desc("Allowed errors per pattern")
                .hasArg().build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helpFormatter.printHelp("Teleporter -f <playerDataFolder> -c <x> <y> <z> -d <targetDimensions>", options);
            System.exit(1);
            return null;
        }
    }
}
