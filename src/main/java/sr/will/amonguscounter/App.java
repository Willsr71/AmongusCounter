package sr.will.amonguscounter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sr.will.amonguscounter.entity.Amongus;
import sr.will.amonguscounter.entity.Image;
import sr.will.amonguscounter.entity.Pattern;
import sr.will.amonguscounter.entity.RawPatterns;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class App {
    public static final Gson GSON = new GsonBuilder().create();
    public final Arguments arguments;

    public Pattern[] patterns;
    public Image image;

    public App(Arguments arguments) throws IOException {
        this.arguments = arguments;

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

        Files.write(Path.of("amongus.json"), GSON.toJson(amonguses).getBytes(StandardCharsets.UTF_8));

        generateOverlayImage(amonguses);
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

    public void indexImage(byte[] pixels) {
        byte[] image = new byte[this.image.width * this.image.height];
        byte[][] indexMap = new byte[32][3];
        byte indexMapLength = 0;

        for (int p = 0; p < image.length; p++) {
            // Get the index of the current pixel
            byte index = -1;
            for (byte i = 0; i < indexMapLength; i++) {
                if (indexMap[i][0] == pixels[p * 3] &&
                        indexMap[i][1] == pixels[(p * 3) + 1] &&
                        indexMap[i][2] == pixels[(p * 3) + 2]) {
                    index = i;
                    break;
                }
            }

            // Index not found, make a new one
            if (index == -1) {
                indexMap[indexMapLength] = new byte[]{
                        pixels[p * 3],
                        pixels[(p * 3) + 1],
                        pixels[(p * 3) + 2]
                };
                index = indexMapLength;
                indexMapLength++;
            }

            image[p] = index;
        }
        this.image = new sr.will.amonguscounter.entity.Image(this.image.width, this.image.height, image);

        Main.LOGGER.info("Index map: {}", (Object) indexMap);
        Main.LOGGER.info("Index map length: {}", indexMapLength);
    }

    public byte[] getImageData() throws IOException {
        BufferedImage bufferedImage = ImageIO.read(arguments.getImage());
        short width = (short) bufferedImage.getWidth();
        short height = (short) bufferedImage.getHeight();
        image = new Image(width, height, new byte[0]);
        return ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
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

    public void generateOverlayImage(List<Amongus> amonguses) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(arguments.getImage());

        BufferedImage overlay = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) overlay.getGraphics();
        graphics.create(0, 0, image.width, image.height);
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRect(0, 0, image.width, image.height);

        graphics.setColor(new Color(0, 0, 0, 0));
        graphics.setComposite(AlphaComposite.Clear);
        for (Amongus amongus : amonguses) {
            graphics.fillRect(amongus.x - 1, amongus.y - 1, patterns[amongus.patternIndex].width + 2, patterns[amongus.patternIndex].height + 2);
        }

        bufferedImage.getGraphics().drawImage(overlay, 0, 0, null);
        ImageIO.write(bufferedImage, "png", new File(arguments.outputImage));
    }
}
