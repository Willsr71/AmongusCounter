package sr.will.amonguscounter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sr.will.amonguscounter.entity.Image;
import sr.will.amonguscounter.entity.*;
import sr.will.amonguscounter.history.HistoryPreProcessor;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class App {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().create();
    public static final byte numColors = 32;

    public static boolean running = true;
    public final Arguments arguments;

    public Metadata metadata;
    public Image image;
    public Graphics2D graphics;
    public Pattern[] patterns;
    public byte[] patternSearchDimensions = new byte[2];

    private DataInputStream inputStream;

    public App(Arguments arguments) throws IOException {
        this.arguments = arguments;
        addShutdownHook();

        preProcessHistory();
        metadata = GSON.fromJson(Files.readString(new File(arguments.workingDirectory, arguments.metadataFile).toPath()), Metadata.class);
        inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(arguments.workingDirectory, arguments.processedHistoryFile))));

        long startTime = System.currentTimeMillis();
        // Initialize canvas
        image = new Image(arguments.width, arguments.height, getInitialImageData());
        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("Initialization took {}ms", endTime - startTime);

        startTime = System.currentTimeMillis();
        patterns = generatePatterns();
        endTime = System.currentTimeMillis();
        Main.LOGGER.info("Pattern generation took {}ms", endTime - startTime);
        for (int i = 0; i < patterns.length; i++) {
            Main.LOGGER.info("Pattern {}: {}", i, patterns[i]);
        }
        Main.LOGGER.info("Pattern search dimensions {}", patternSearchDimensions);

        // Process image
        for (int i = 0; i < metadata.records; i++) {
            processPixel();

            if (!running) break;
        }

        inputStream.close();
    }

    private void processPixel() throws IOException {
        long timestamp = inputStream.readLong();
        byte colorIndex = inputStream.readByte();
        short x = inputStream.readShort();
        short y = inputStream.readShort();
        short endX = inputStream.readShort();
        short endY = inputStream.readShort();

        //Main.LOGGER.info("Processing pixel with timestamp {}, color {}, ({}, {}), ({}, {})", timestamp, colorIndex, x, y, endX, endY);

        if (endX == 0 && endY == 0) {
            placePixel(colorIndex, x, y);
            getAmongi(
                    (short) Math.max(0, x - patternSearchDimensions[0]),
                    (short) Math.max(0, y - patternSearchDimensions[1]),
                    (short) Math.min(image.width, x + patternSearchDimensions[0]),
                    (short) Math.min(image.height, y + patternSearchDimensions[1])
            );
        } else {
            placePixelRange(colorIndex, x, y, endX, endY);
        }
    }

    private void placePixel(byte colorIndex, short x, short y) {
        image.data[(y * image.width) + x] = colorIndex;
    }

    private void placePixelRange(byte colorIndex, short x, short y, short endX, short endY) {
        byte[] stripe = new byte[endX - x];
        Arrays.fill(stripe, colorIndex);

        for (short i = y; i < endY; i++) {
            System.arraycopy(stripe, 0, image.data, (i * image.width) + x, endX - x);
        }
    }

    public List<Amongus> getAmongi(short startX, short startY, short endX, short endY) {
        //Main.LOGGER.info("({}, {}, {}, {}), locating over ~{} loops...", startX, startY, endX, endY, (endX - startX) * (endY - startY) * patterns.length);

        List<Amongus> amonguses = new ArrayList<>();
        int count = 0;
        for (byte patternIndex = 0; patternIndex < patterns.length; patternIndex++) {
            for (short y = startY; y + patterns[patternIndex].height < endY; y++) {
                for (short x = startX; x + patterns[patternIndex].width < endX; x++) {
                    count++;
                    if (getAmongus(x, y, patterns[patternIndex])) {
                        amonguses.add(new Amongus(x, y, patternIndex));
                    }
                }
            }
        }

        //Main.LOGGER.info("Iterations: {}", count);
        //Main.LOGGER.info("Found: {}", amonguses.size());
        return amonguses;
    }

    public boolean getAmongus(short imageX, short imageY, Pattern pattern) {
        byte errors = 0;

        // Get color of a primary pixel
        int imageIndex = (imageY * image.width) + imageX;
        byte primaryColor = image.data[imageIndex + pattern.primaryColorIndex];
        byte secondaryColor = -1;

        for (short i = 0, x = 0, y = 0; i < pattern.width * pattern.height; i++) {
            byte pixel = image.data[imageIndex + x];

            if (pattern.data[i] == -1) {
                // Pixel is safe to ignore
                continue;
            } else if (pattern.data[i] == 1 && primaryColor != pixel || pattern.data[i] == 0 && primaryColor == pixel) {
                // Primary pixel is not primary color or if blank pixel is primary color
                errors++;
                if (errors > arguments.allowedErrors) return false;
            } else if (pattern.data[i] == 2) {
                // Secondary color is not set
                if (secondaryColor == -1) {
                    // Secondary color cannot be the same color as the primary color
                    if (primaryColor == pixel) return false;
                    else secondaryColor = pixel;
                } else {
                    // secondary colors do not match
                    if (secondaryColor != pixel) return false;
                }
            }

            x++;
            if (x == pattern.width) {
                x = 0;
                y++;
                imageIndex = (imageY * image.width) + imageX + (image.width * y);
            }
        }

        Main.LOGGER.info("Found amongus at {}, {}", imageX, imageY);
        for (int y = 0; y < pattern.height; y++) {
            int start = (imageY * image.width) + imageX + (image.width * y);
            Main.LOGGER.info("{}", Arrays.copyOfRange(image.data, start, start + pattern.width));
        }

        return true;
    }

    public Pattern[] generatePatterns() {
        RawPatterns rawPatterns = GSON.fromJson(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/amongus.json"))), RawPatterns.class);

        Pattern[] patterns = new Pattern[rawPatterns.patterns.size() * 2];

        byte patternIndex = 0;
        for (RawPatterns.RawPattern pattern : rawPatterns.patterns) {
            byte height = (byte) pattern.pattern.size();
            byte width = (byte) pattern.pattern.get(0).length();

            if (width > patternSearchDimensions[0]) patternSearchDimensions[0] = width;
            if (height > patternSearchDimensions[1]) patternSearchDimensions[1] = height;

            byte[][] currentPatterns = new byte[2][width * height];
            for (int i = 0, x = 0, y = 0; i < width * height; i++) {
                char c = pattern.pattern.get(y).charAt(x);
                byte result;
                if (c == 'X') result = 1;
                else if (c == 'O') result = 2;
                else result = 0;

                currentPatterns[0][(y * width) + x] = result;
                currentPatterns[1][(y * width) + (width - x - 1)] = result;

                x++;
                if (x == width) {
                    x = 0;
                    y++;
                }
            }

            patterns[patternIndex] = new Pattern(pattern.name, width, height, currentPatterns[0]);
            patterns[patternIndex + 1] = new Pattern(pattern.name, width, height, currentPatterns[1]);
            patternIndex += 2;
        }

        return patterns;
    }

    public byte[] getInitialImageData() {
        byte whiteIndex = metadata.getColorIndex(new Color(255, 255, 255).getRGB());
        byte[] imageData = new byte[arguments.width * arguments.height];
        Arrays.fill(imageData, whiteIndex);
        return imageData;
    }

    private void preProcessHistory() {
        File processedHistoryFile = new File(arguments.workingDirectory, arguments.processedHistoryFile);
        if (arguments.forcePreProcess || !processedHistoryFile.exists()) {
            HistoryPreProcessor preProcessor = new HistoryPreProcessor(arguments);

            try {
                preProcessor.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Main.LOGGER.info("Stopping...");
            running = false;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }
}
