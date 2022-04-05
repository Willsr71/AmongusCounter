package sr.will.amonguscounter;

public class Amongus {
    public short x;
    public short y;
    public byte width;
    public byte height;

    public Amongus(short x, short y, Image pattern) {
        this.x = x;
        this.y = y;
        this.width = (byte) pattern.width;
        this.height = (byte) pattern.height;
    }

    public String toString() {
        return "Image[x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                "]";
    }
}
