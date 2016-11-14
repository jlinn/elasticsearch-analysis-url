package org.elasticsearch.index.analysis.url;

import org.elasticsearch.index.analysis.URLPart;

import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Joe Linn
 * 7/30/2015
 */
public class URLUtils {
    private static final Pattern PATTERN_PROTOCOL = Pattern.compile("(^[a-zA-Z]*)://");
    private static final Pattern PATTERN_HOST = Pattern.compile("^(?:^[a-zA-Z]*://)?((?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\\.?)/?(?:.*)");
    private static final Pattern PATTERN_PORT = Pattern.compile("^(?:^[a-zA-Z]*://)?(?:(?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\\.?)(?::([0-9]*))?/?(?:.*)");
    private static final Pattern PATTERN_PATH = Pattern.compile("(?:^[a-zA-Z]*://)?(?:(?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\\.?)?(?::[0-9]*)?([^\\?\\#&]*)");
    private static final Pattern PATTERN_REF = Pattern.compile("(?:^[a-zA-Z]*://)?(?:(?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\\.?)?(?::[0-9]*)?(?:[^\\?\\#&]*)(#[^\\?\\&]*)?");
    private static final Pattern PATTERN_QUERY = Pattern.compile("(?:^[a-zA-Z]*://)?(?:(?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\\.?)?(?::[0-9]*)?(?:[^\\?\\#&]*)(?:#[^\\?\\&]*)?(\\?.*)");

    private URLUtils() {}


    /**
     * Attempt to retrieve the desired part of the given URL
     * @param url URL to parse
     * @param part desired URL part
     * @return the part of the URL, if it could be found
     */
    public static Optional<String> getPart(String url, URLPart part) {
        switch (part) {
            case PROTOCOL:
                return captureFirst(url, PATTERN_PROTOCOL);
            case HOST:
                return captureFirst(url, PATTERN_HOST);
            case PORT:
                return getPort(url);
            case PATH:
                return captureFirst(url, PATTERN_PATH);
            case REF:
                Optional<String> refOptional = captureFirst(url, PATTERN_REF);
                if (refOptional.isPresent()) {
                    refOptional = Optional.of(refOptional.get().replaceFirst("#", ""));
                }
                return refOptional;
            case QUERY:
                Optional<String> queryOptional = captureFirst(url, PATTERN_QUERY);
                if (queryOptional.isPresent()) {
                    queryOptional = Optional.of(queryOptional.get().replaceFirst("\\?", ""));
                }
                return queryOptional;
            case WHOLE:
            default:
                return Optional.of(url);
        }
    }


    private static Optional<String> captureFirst(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String group = matcher.group(1);
            if (group == null) {
                return Optional.empty();
            }
            return Optional.of(group);
        }
        return Optional.empty();
    }


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
            Optional<String> portOptional = portFromProtocol(url.getProtocol());
            return portOptional.orElse(null);
        }
        return String.valueOf(port);
    }


    public static Optional<String> getPort(String url) {
        Optional<String> portOptional = captureFirst(url, PATTERN_PORT);
        if (portOptional.isPresent()) {
            return portOptional;
        }
        // attempt to infer port form protocol
        Optional<String> protocolOptional = getPart(url, URLPart.PROTOCOL);
        if (protocolOptional.isPresent()) {
            return portFromProtocol(protocolOptional.get());
        }
        return Optional.empty();
    }


    private static Optional<String> portFromProtocol(final String protocol) {
        int port = -1;
        if (protocol.equals("http")) {
            port = 80;
        } else if (protocol.equals("https")) {
            port = 443;
        }
        if (port == -1) {
            // port could not be inferred
            return Optional.empty();
        }
        return Optional.of(String.valueOf(port));
    }
}
