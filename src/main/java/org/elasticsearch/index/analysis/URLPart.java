package org.elasticsearch.index.analysis;

import com.google.common.collect.Ordering;

/**
 * Joe Linn
 * 1/17/2015
 */
public enum URLPart {
    PROTOCOL,
    HOST,
    PORT,
    PATH,
    REF,
    QUERY,
    WHOLE;

    public static URLPart fromString(String part) {
        for (URLPart urlPart : URLPart.values()) {
            if (urlPart.name().equalsIgnoreCase(part)) {
                return urlPart;
            }
        }
        throw new IllegalArgumentException(String.format("Unrecognized URL part: %s", part));
    }

    public static Ordering<URLPart> PART_ORDER = Ordering.explicit(PROTOCOL, HOST, PORT, PATH, REF, QUERY, WHOLE);
}
