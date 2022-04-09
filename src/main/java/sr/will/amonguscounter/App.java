package sr.will.amonguscounter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sr.will.amonguscounter.entity.Amongus;
import sr.will.amonguscounter.entity.Image;
import sr.will.amonguscounter.entity.Pattern;
import sr.will.amonguscounter.entity.RawPatterns;
import sr.will.amonguscounter.history.HistoryPreProcessor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class App {
    public static final Gson GSON = new GsonBuilder().create();
    public static boolean running = true;
    public final Arguments arguments;

    public Image image;
    public Pattern[] patterns;

    public App(Arguments arguments) {
        this.arguments = arguments;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Main.LOGGER.info("Stopping...");
            running = false;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        if (arguments.forcePreProcess || !arguments.historyFile.exists()) {
            HistoryPreProcessor preProcessor = new HistoryPreProcessor(arguments.rawHistoryFile, arguments.historyFile);

            try {
                preProcessor.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
        long startTime = System.currentTimeMillis();
        patterns = generatePatterns();
        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("Pattern generation took {}ms", endTime - startTime);
        for (int i = 0; i < patterns.length; i++) {
            Main.LOGGER.info("Pattern {}: {}", i, patterns[i]);
        }

        startTime = System.currentTimeMillis();
        byte[] imageData = getImageData();
        endTime = System.currentTimeMillis();
        Main.LOGGER.info("Image reading took {}ms", endTime - startTime);


        startTime = System.currentTimeMillis();
        indexImage(imageData);
        endTime = System.currentTimeMillis();
        Main.LOGGER.info("Image indexing took {}ms", endTime - startTime);

        Main.LOGGER.info("Attempting to find amongus, width: {}, height: {}, patterns: {},", image.width, image.height, patterns.length);
        startTime = System.currentTimeMillis();
        List<Amongus> amonguses = getAmongi((short) 0, (short) 0, image.width, image.height);
        endTime = System.currentTimeMillis();
        Main.LOGGER.info("Locating took {}ms", endTime - startTime);
         */
    }

    public List<Amongus> getAmongi(short startX, short startY, short endX, short endY) {
        Main.LOGGER.info("Locating over ~{} loops...", (endX - startX) * (endY - startY) * patterns.length);

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

        Main.LOGGER.info("Iterations: {}", count);
        Main.LOGGER.info("Found: {}", amonguses.size());
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

            // Primary pixel is not primary color or if blank pixel is primary color
            if (pattern.data[i] == 1 && primaryColor != pixel || pattern.data[i] == 0 && primaryColor == pixel) {
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

        return true;
    }

    public Pattern[] generatePatterns() {
        RawPatterns rawPatterns = GSON.fromJson(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/amongus.json"))), RawPatterns.class);

        Pattern[] patterns = new Pattern[rawPatterns.patterns.size() * 2];

        byte patternIndex = 0;
        for (RawPatterns.RawPattern pattern : rawPatterns.patterns) {
            byte height = (byte) pattern.pattern.size();
            byte width = (byte) pattern.pattern.get(0).length();

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
}
