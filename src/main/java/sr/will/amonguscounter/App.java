package sr.will.amonguscounter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static final Gson GSON = new GsonBuilder().create();
    public final Arguments arguments;

    public Image[] patterns;
    public Image image;

    long startTime;
    long endTime;

    public App(Arguments arguments) throws IOException {
        this.arguments = arguments;

        startTime = System.currentTimeMillis();
        //for (int i = 0; i < 1000; i++) {
        generatePatterns();
        //}
        endTime = System.currentTimeMillis();
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
        //Main.LOGGER.info("Image: {}", (Object) image);

        Main.LOGGER.info("Attempting to find amongus, width: {}, height: {}, patterns: {},", image.width, image.height, patterns.length);
        Main.LOGGER.info("Locating over ~{} loops...", image.width * image.height * patterns.length);
        startTime = System.currentTimeMillis();
        List<Amongus> amonguses = getAmongi();
        endTime = System.currentTimeMillis();
        Main.LOGGER.info("Locating took {}ms", endTime - startTime);

        Files.write(Path.of("amongus.json"), GSON.toJson(amonguses).getBytes(StandardCharsets.UTF_8));

        generateOverlayImage(amonguses);
    }

    public List<Amongus> getAmongi() {
        List<Amongus> amonguses = new ArrayList<>();
        int count = 0;
        for (byte pattern = 0; pattern < patterns.length; pattern++) {
            for (short y = 0; y + patterns[pattern].height < image.height; y++) {
                for (short x = 0; x + patterns[pattern].width < image.width; x++) {
                    count++;
                    if (getAmongus(x, y, patterns[pattern])) {
                        amonguses.add(new Amongus(x, y, patterns[pattern]));
                    }
                }
            }
        }

        Main.LOGGER.info("Iterations: {}", count);
        Main.LOGGER.info("Found: {}", amonguses.size());
        return amonguses;
    }

    public boolean getAmongus(int imageX, int imageY, Image pattern) {
        byte primary = -1;
        byte errors = 0;

        for (byte y = 0; y < pattern.height; y++) {
            for (byte x = 0; x < pattern.width; x++) {
                byte pixel = image.data[((imageY + y) * image.width) + imageX + x];
                boolean isPrimary = pattern.data[(y * pattern.width) + x] != 0;

                // We have not detected a primary pixel yet
                if (primary == -1) {
                    // This is not a primary pixel in the pattern, ignore
                    if (pattern.data[(y * pattern.width) + x] != 0) {
                        primary = pixel;
                    }
                    continue;
                }

                if (isPrimary && primary != pixel ||
                        !isPrimary && primary == pixel) {
                    errors++;
                    if (errors > arguments.allowedErrors) return false;
                }
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
        this.image = new Image(this.image.width, this.image.height, image);

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

    public void generatePatterns() throws IOException {
        File[] files = arguments.getPatternsFolder().listFiles();
        assert files != null;

        patterns = new Image[files.length * 2];

        for (int i = 0; i < files.length; i++) {
            BufferedImage image = ImageIO.read(files[i]);
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            short width = (short) image.getWidth();
            short height = (short) image.getHeight();

            byte[][] patterns = new byte[2][width * height];
            for (int p = 0, x = 0, y = 0; p + 2 < pixels.length; p += 3) {
                byte result;
                if (pixels[p] == 0) result = 1;
                else result = 0;
                patterns[0][(y * width) + x] = result;
                patterns[1][(y * width) + (width - x - 1)] = result;
                //patterns[2][((height - y - 1) * width) + x] = result;
                //patterns[3][(height - y - 1) + (width - x - 1)] = result;
                x++;
                if (x == width) {
                    x = 0;
                    y++;
                }
            }

            this.patterns[i * 2] = new Image(width, height, patterns[0]);
            this.patterns[(i * 2) + 1] = new Image(width, height, patterns[1]);
            //this.patterns[(i * 4) + 2] = new Image(width, height, patterns[2]);
            //this.patterns[(i * 4) + 3] = new Image(width, height, patterns[3]);
        }
    }

    public void generateOverlayImage(List<Amongus> amonguses) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(arguments.getImage());

        BufferedImage overlay = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) overlay.getGraphics();
        graphics.create(0, 0, image.width, image.height);
        graphics.setColor(new Color(0, 0, 0, 192));
        graphics.fillRect(0, 0, image.width, image.height);

        graphics.setColor(new Color(0, 0, 0, 0));
        graphics.setComposite(AlphaComposite.Clear);
        for (Amongus amongus : amonguses) {
            graphics.fillRect(amongus.x - 1, amongus.y - 1, amongus.width + 2, amongus.height + 2);
        }

        bufferedImage.getGraphics().drawImage(overlay, 0, 0, null);
        ImageIO.write(bufferedImage, "png", new File("overlay.png"));
    }
}
