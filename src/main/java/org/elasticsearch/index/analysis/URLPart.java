package org.elasticsearch.index.analysis;

/**
 * Joe Linn
 * 1/17/2015
 */
public enum URLPart {
    PROTOCOL((short) 1),
    HOST((short) 2),
    PORT((short) 3),
    PATH((short) 4),
    REF((short) 5),
    QUERY((short) 6),
    WHOLE((short) 7);

    private final short order;

    URLPart(short order) {
        this.order = order;
    }

    public short getOrder() {
        return order;
    }

    public static URLPart fromString(String part) {
        for (URLPart urlPart : URLPart.values()) {
            if (urlPart.name().equalsIgnoreCase(part)) {
                return urlPart;
            }
        }
        throw new IllegalArgumentException(String.format("Unrecognized URL part: %s", part));
    }
}
