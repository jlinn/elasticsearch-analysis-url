package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.analysis.URLPart;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Joe Linn
 * 1/17/2015
 */
public final class URLTokenFilter extends TokenFilter {
    public static final String NAME = "url";

    private final URLPart part;

    private final boolean urlDeocde;

    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

    private final boolean allowMalformed;

    public URLTokenFilter(TokenStream input, URLPart part) {
        this(input, part, false);
    }

    public URLTokenFilter(TokenStream input, URLPart part, boolean urlDecode) {
        this(input, part, urlDecode, false);
    }

    public URLTokenFilter(TokenStream input, URLPart part, boolean urlDecode, boolean allowMalformed) {
        super(input);
        this.part = part;
        this.urlDeocde = urlDecode;
        this.allowMalformed = allowMalformed;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            final String urlString = termAttribute.toString();
            termAttribute.setEmpty();
            if (Strings.isNullOrEmpty(urlString) || urlString.equals("null")) {
                return false;
            }
            String partString;
            try {
                URL url = new URL(urlString);
                switch (part) {
                    case PROTOCOL:
                        partString = url.getProtocol();
                        break;
                    case HOST:
                        partString = url.getHost();
                        break;
                    case PORT:
                        partString = parsePort(url);
                        break;
                    case PATH:
                        partString = url.getPath();
                        break;
                    case REF:
                        partString = url.getRef();
                        break;
                    case QUERY:
                        partString = url.getQuery();
                        break;
                    case WHOLE:
                    default:
                        partString = url.toString();
                }
            } catch (MalformedURLException e) {
                if (allowMalformed) {
                    partString = parseMalformed(urlString);
                    if (Strings.isNullOrEmpty(partString)) {
                        return false;
                    }
                } else {
                    throw e;
                }
            }
            if (urlDeocde) {
                partString = URLDecoder.decode(partString, "UTF-8");
            }
            termAttribute.append(partString);
            return true;
        }
        return false;
    }

    private static final Pattern REGEX_PROTOCOL = Pattern.compile("^([a-zA-Z]+)(?=://)");
    private static final Pattern REGEX_PORT = Pattern.compile(":([0-9]{1,5})");
    private static final Pattern REGEX_QUERY = Pattern.compile("\\?(.+)");

    /**
     * Attempt to parse a malformed url string
     * @param urlString the malformed url string
     * @return the url part if it can be parsed, null otherwise
     */
    private String parseMalformed(String urlString) {
        switch (part) {
            case PROTOCOL:
                return applyPattern(REGEX_PROTOCOL, urlString);
            case PORT:
                return applyPattern(REGEX_PORT, urlString);
            case QUERY:
                return applyPattern(REGEX_QUERY, urlString);
            case WHOLE:
                return urlString;
            default:
                return null;
        }
    }

    /**
     * Apply the given regex pattern to the given malformed url string and return the first match
     * @param pattern the pattern to match
     * @param urlString the malformed url to which the pattern should be applied
     * @return the first match if one exists, null otherwise
     */
    private String applyPattern(Pattern pattern, String urlString) {
        Matcher matcher = pattern.matcher(urlString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


    /**
     * Parse the port from the given {@link URL}. If the port is not explicitly given, it will be inferred from the
     * protocol.
     *
     * @param url the url
     * @return the port
     */
    private String parsePort(URL url) {
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
