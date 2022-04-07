package sr.will.amonguscounter.entity;

import java.io.Serializable;
import java.util.List;

public class RawPatterns implements Serializable {
    public List<RawPattern> patterns;

    public static class RawPattern {
        public String name;
        public List<String> pattern;
    }
}
