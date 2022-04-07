package sr.will.amonguscounter.entity;

import java.util.Arrays;

public class Pattern {
    public final String name;
    public final byte width;
    public final byte height;
    public final short primaryColorIndex;
    public final short secondaryColorIndex;
    public final byte[] data;

    public Pattern(String name, byte width, byte height, byte[] data) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.data = data;

        this.primaryColorIndex = getColorIndex((byte) 1);
        this.secondaryColorIndex = getColorIndex((byte) 2);
    }

    private short getColorIndex(byte color) {
        for (short i = 0; i < width * height; i++) {
            if (data[i] == color) return i;
        }
        throw new RuntimeException("No color " + color + " found for pattern " + this);
    }

    public String toString() {
        return "Pattern[width=" + width +
                ", height=" + height +
                ", primaryColorIndex=" + primaryColorIndex +
                ", secondaryColorIndex=" + secondaryColorIndex +
                ", data=" + Arrays.toString(data) +
                ", name=" + name +
                "]";
    }
}
