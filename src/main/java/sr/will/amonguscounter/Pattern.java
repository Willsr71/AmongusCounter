package sr.will.amonguscounter;

import java.util.Arrays;

public class Pattern {
    public final byte width;
    public final byte height;
    public final byte primaryColorX;
    public final byte primaryColorY;
    public final byte[] data;

    public Pattern(byte width, byte height, byte[] data) {
        this.width = width;
        this.height = height;
        this.data = data;

        byte[] primaryColorCoords = getPrimaryColorIndex();
        this.primaryColorX = primaryColorCoords[0];
        this.primaryColorY = primaryColorCoords[1];
    }

    private byte[] getPrimaryColorIndex() {
        for (short i = 0, x = 0, y = 0; i < width * height; i++) {
            if (data[i] != 0) return new byte[]{(byte) x, (byte) y};

            x++;
            if (x == width) {
                x = 0;
                y++;
            }
        }
        throw new RuntimeException("No primary color found for pattern " + this);
    }

    public String toString() {
        return "Image[width=" + width +
                ", height=" + height +
                ", primaryColorX=" + primaryColorX +
                ", primaryColorY=" + primaryColorY +
                ", data=" + Arrays.toString(data) +
                "]";
    }
}
