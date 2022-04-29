package sr.will.amonguscounter.entity;

import sr.will.amonguscounter.App;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;

public class ImageCreator {

    public static void createImage(long timestamp, Color[] colors, Image image, short startX, short startY, short width, short height) throws IOException {
        byte[] data = new byte[width * height];
        for (int y = 0; y < height; y++) {
            System.arraycopy(image.data, ((y + startY) * image.width) + startX, data, y * width, width);
        }

        createImage(timestamp, colors, data, width, height);
    }

    public static void createImage(long timestamp, Color[] colors, byte[] image, short width, short height) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        DataBuffer dataBuffer = bufferedImage.getRaster().getDataBuffer();

        for (int i = 0; i < width * height; i++) {
            dataBuffer.setElem(i, colors[image[i]].getRGB());
        }

        ImageIO.write(bufferedImage, "png", new File(App.arguments.workingDirectory, "images/" + timestamp + ".png"));
    }
}
