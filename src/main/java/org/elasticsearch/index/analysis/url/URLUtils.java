package org.elasticsearch.index.analysis.url;

import org.elasticsearch.index.analysis.URLPart;

import java.net.URL;

/**
 * Joe Linn
 * 7/30/2015
 */
public class URLUtils {
    private URLUtils() {}

    /**
     * Retrieve the given {@link URLPart} from the given {@link URL}
     * @param url the url from which a part is to be taken
     * @param part the part to be taken from the url
     * @return a part of the given url
     */
    public static String getPart(URL url, URLPart part) {
        switch (part) {
            case PROTOCOL:
                return url.getProtocol();
            case HOST:
                return url.getHost();
            case PORT:
                return getPort(url);
            case PATH:
                return url.getPath();
            case REF:
                return url.getRef();
            case QUERY:
                return url.getQuery();
            case WHOLE:
            default:
                return url.toString();
        }
    }


    /**
     * Parse the port from the given {@link URL}. If the port is not explicitly given, it will be inferred from the
     * protocol.
     *
     * @param url the url
     * @return the port
     */
    public static String getPort(URL url) {
        int port = url.getPort();
        if (port == -1) {
            // infer port from protocol
            final String protocol = url.getProtocol();
            if (protocol.equals("http")) {
                port = 80;
            } else if (protocol.equals("https")) {
                port = 443;
            }
        }
        return String.valueOf(port);
    }
}
