package sr.will.amonguscounter.entity;

import sr.will.amonguscounter.App;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Metadata {
    public int records;
    public Map<Byte, ColorInfo> colorInfo = new HashMap<>();

    public Metadata(int[] colorsIndex, int[] colorUsage, byte[] colorRemaps) {
        for (byte i = 0; i < App.numColors; i++) {
            records += colorUsage[i];
            colorInfo.put(i, new ColorInfo(
                    i,
                    colorsIndex[colorRemaps[i]],
                    colorUsage[colorRemaps[i]]
            ));
        }
    }

    public Color[] getColors() {
        Color[] colors = new Color[App.numColors];
        for (byte i = 0; i < App.numColors; i++) {
            colors[i] = new Color(colorInfo.get(i).color);
        }
        return colors;
    }

    public byte getColorIndex(int color) {
        color = new Color(color).getRGB();
        for (byte i = 0; i < App.numColors; i++) {
            if (new Color(colorInfo.get(i).color).getRGB() == color) return i;
        }
        return -1;
    }
}
