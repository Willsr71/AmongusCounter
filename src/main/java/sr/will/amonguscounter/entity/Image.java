package sr.will.amonguscounter.entity;

public class Image {
    public final short width;
    public final short height;
    public byte[] data;

    public Image(short width, short height, byte[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public String toString() {
        return "Image[width=" + width +
                ", height=" + height +
                "]";
    }
}
