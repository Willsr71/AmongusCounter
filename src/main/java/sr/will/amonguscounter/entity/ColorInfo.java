package sr.will.amonguscounter.entity;

import java.io.Serializable;

public class ColorInfo implements Serializable {
    public byte index;
    public int color;
    public int uses;

    public ColorInfo(byte index, int color, int uses) {
        this.index = index;
        this.color = color;
        this.uses = uses;
    }
}
