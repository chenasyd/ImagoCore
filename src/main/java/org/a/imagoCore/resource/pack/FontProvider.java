package org.a.imagoCore.resource.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Represents a single entry in the {@code font/default.json} "providers" array.
 *
 * <p>Serialised via {@link #toJson()} into either a {@code "space"} or
 * {@code "bitmap"} provider object.
 */
public class FontProvider {

    private final String type;
    private final String file;       // null for space providers
    private final int ascent;        // ignored for space
    private final int height;        // ignored for space
    private final String[] chars;    // single-char array for bitmap, advance map for space
    private final int advance;       // only for space type

    private FontProvider(Builder builder) {
        this.type = builder.type;
        this.file = builder.file;
        this.ascent = builder.ascent;
        this.height = builder.height;
        this.chars = builder.chars;
        this.advance = builder.advance;
    }

    public static Builder space(String character, int advance) {
        return new Builder("space")
                .chars(new String[]{character})
                .advance(advance);
    }

    public static Builder bitmap(String file, int ascent, int height, String character) {
        return new Builder("bitmap")
                .file(file)
                .ascent(ascent)
                .height(height)
                .chars(new String[]{character});
    }

    /** Serialise to a Gson element. */
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);

        if ("space".equals(type)) {
            JsonObject advances = new JsonObject();
            advances.addProperty(chars[0], advance);
            obj.add("advances", advances);
        } else {
            obj.addProperty("file", file);
            obj.addProperty("ascent", ascent);
            obj.addProperty("height", height);
            JsonArray charsArray = new JsonArray();
            charsArray.add(chars[0]);
            obj.add("chars", charsArray);
        }

        return obj;
    }

    // ── Builder ─────────────────────────────────────────────────

    public static class Builder {
        private final String type;
        private String file;
        private int ascent;
        private int height;
        private String[] chars;
        private int advance;

        Builder(String type) {
            this.type = type;
        }

        public Builder file(String file) {
            this.file = file;
            return this;
        }

        public Builder ascent(int ascent) {
            this.ascent = ascent;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder chars(String[] chars) {
            this.chars = chars;
            return this;
        }

        public Builder advance(int advance) {
            this.advance = advance;
            return this;
        }

        public FontProvider build() {
            return new FontProvider(this);
        }
    }
}
