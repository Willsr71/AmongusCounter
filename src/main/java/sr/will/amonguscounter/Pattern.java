package sr.will.amonguscounter;

import java.util.Arrays;

public class Pattern {
    public final byte width;
    public final byte height;
    public final short primaryColorIndex;
    public final byte[] data;

    public Pattern(byte width, byte height, byte[] data) {
        this.width = width;
        this.height = height;
        this.data = data;

        this.primaryColorIndex = getPrimaryColorIndex();
    }

    private short getPrimaryColorIndex() {
        for (short i = 0; i < width * height; i++) {
            if (data[i] != 0) return i;
        }
        throw new RuntimeException("No primary color found for pattern " + this);
    }

    public String toString() {
        return "Image[width=" + width +
                ", height=" + height +
                ", primaryColorIndex=" + primaryColorIndex +
                ", data=" + Arrays.toString(data) +
                "]";
    }
}
