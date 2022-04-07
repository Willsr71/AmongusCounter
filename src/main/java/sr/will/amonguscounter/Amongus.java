package sr.will.amonguscounter;

public class Amongus {
    public short x;
    public short y;
    public byte patternIndex;

    public Amongus(short x, short y, byte patternIndex) {
        this.x = x;
        this.y = y;
        this.patternIndex = patternIndex;
    }

    public String toString() {
        return "Image[x=" + x +
                ", y=" + y +
                "]";
    }
}
