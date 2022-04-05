package sr.will.amonguscounter;

import java.util.Arrays;

public class Image {
    public final short width;
    public final short height;
    public final byte[] data;

    public Image(short width, short height, byte[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public String toString() {
        return "Image[width=" + width +
                ", height=" + height +
                ", data=" + Arrays.toString(data) +
                "]";
    }
}
