package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.path.PathHierarchyTokenizer;
import org.apache.lucene.analysis.path.ReversePathHierarchyTokenizer;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.net.InetAddresses;
import org.elasticsearch.index.analysis.URLPart;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static org.elasticsearch.index.analysis.url.URLUtils.getPart;
import static org.elasticsearch.index.analysis.url.URLUtils.getPort;

/**
 * Joe Linn
 * 7/30/2015
 */
public final class URLTokenizer extends Tokenizer {
    public static final String NAME = "url";

    /**
     * If set, only the given part of the url will be tokenized.
     */
    private URLPart part;

    /**
     * If true, url parts will be url decoded prior to tokenization.
     */
    private boolean urlDecode;

    /**
     * If true, the url's host will be tokenized using a {@link ReversePathHierarchyTokenizer}
     */
    private boolean tokenizeHost = true;

    /**
     * If true, the url's path will be tokenized using a {@link PathHierarchyTokenizer}
     */
    private boolean tokenizePath = true;

    /**
     * If true, {@link MalformedURLException} will be suppressed, and the given string will be returned as a single token
     */
    private boolean allowMalformed;


    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);

    private List<Token> tokens;
    private Iterator<Token> iterator;


    public URLTokenizer(Reader input) {
        super(input);
    }

    public URLTokenizer(Reader input, URLPart part) {
        this(input);
        this.part = part;
    }


    public URLTokenizer(AttributeFactory factory, Reader input) {
        super(factory, input);
    }


    public void setPart(URLPart part) { this.part = part; }

    public void setUrlDecode(boolean urlDecode) { this.urlDecode = urlDecode; }

    public void setTokenizeHost(boolean tokenizeHost) { this.tokenizeHost = tokenizeHost; }

    public void setTokenizePath(boolean tokenizePath) { this.tokenizePath = tokenizePath; }

    public void setAllowMalformed(boolean allowMalformed) { this.allowMalformed = allowMalformed; }


    @Override
    public boolean incrementToken() throws IOException {
        if (iterator == null) {
            String urlString = readerToString(input);
            if (Strings.isNullOrEmpty(urlString)) {
                return false;
            }
            tokens = tokenize(urlString);
            iterator = tokens.iterator();
        }
        if (!iterator.hasNext()) {
            return false;
        }

        clearAttributes();
        Token token = iterator.next();
        termAttribute.append(token.getToken());
        typeAttribute.setType(token.getPart().name().toLowerCase());
        return true;
    }


    @Override
    public void reset() throws IOException {
        super.reset();
        tokens = null;
        iterator = null;
    }


    /**
     * Read the contents of a {@link Reader} into a string
     * @param reader the reader to be converted
     * @return the entire contents of the given reader
     * @throws IOException
     */
    private String readerToString(Reader reader) throws IOException {
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        return buffer.toString();
    }


    /**
     * Tokenize the given URL string according to the options which have been set.
     * @param urlString the string to be tokenized
     * @return a list of {@link Token}s parsed from the string
     * @throws IOException
     */
    private List<Token> tokenize(String urlString) throws IOException {
        try {
            URL url = new URL(urlString);
            if (part != null) {
                // single URL part
                return tokenize(url, part);
            }
            // No part is specified. Tokenize all parts.
            List<Token> tokens = new ArrayList<>();
            for (URLPart urlPart : URLPart.values()) {
                tokens.addAll(tokenize(url, urlPart));
            }
            tokens.addAll(tokenizeSpecial(url));
            return tokens;
        } catch (MalformedURLException e) {
            if (allowMalformed) {
                return ImmutableList.of(new Token(urlString, URLPart.WHOLE));
            }
            throw new IOException("Malformed URL: " + urlString, e);
        }
    }


    private static final Pattern QUERY_SEPARATOR = Pattern.compile("&");

    /**
     * Tokenize the given {@link URL} based on the desired {@link URLPart} and currently set tokenizer options.
     * @param url the url to be tokenized
     * @param part the desired part of the url
     * @return a list of {@link Token}s parsed from the given url
     * @throws IOException
     */
    private List<Token> tokenize(URL url, URLPart part) throws IOException {
        String partString = getPart(url, part);
        if (urlDecode) {
            partString = URLDecoder.decode(partString, "UTF-8");
        }
        switch (part) {
            case HOST:
                if (!tokenizeHost || InetAddresses.isInetAddress(partString)) {
                    return ImmutableList.of(new Token(partString, part));
                }
                return tokenize(part, new ReversePathHierarchyTokenizer(new StringReader(partString), '.', '.'));
            case PORT:
                return ImmutableList.of(new Token(getPort(url), part));
            case PATH:
                if (!tokenizePath) {
                    return ImmutableList.of(new Token(partString, part));
                }
                return tokenize(part, new PathHierarchyTokenizer(new StringReader(partString), '/', '/'));
            case QUERY:
                return tokenize(part, new PatternTokenizer(new StringReader(partString), QUERY_SEPARATOR, -1));
            case PROTOCOL:
            case REF:
            case WHOLE:
            default:
                return ImmutableList.of(new Token(partString, part));
        }
    }


    /**
     * Get a list of {@link Token}s from the given {@link Tokenizer}
     * @param part the url part which should be used in {@link Token} creation
     * @param tokenizer the tokenizer from which tokens will be gleaned
     * @return a list of tokens
     * @throws IOException
     */
    List<Token> tokenize(URLPart part, Tokenizer tokenizer) throws IOException {
        tokenizer.reset();
        List<Token> tokens = new ArrayList<>();
        while (tokenizer.incrementToken()) {
            tokens.add(new Token(tokenizer.getAttribute(CharTermAttribute.class).toString(), part));
        }
        return tokens;
    }


    /**
     * Perform non-standard tokenization.
     * @param url the URL to be tokenized
     * @return a list of {@link Token}s. Since tokens created in this method do not pertain to a specific part of the url,
     * {@link URLPart#WHOLE} will be used.
     */
    List<Token> tokenizeSpecial(URL url) {
        List<Token> tokens = new ArrayList<>();
        // host:port
        tokens.add(new Token(getPart(url, URLPart.HOST) + ":" + getPart(url, URLPart.PORT), URLPart.WHOLE));
        // protocol://host
        tokens.add(new Token(getPart(url, URLPart.PROTOCOL) + "://" + getPart(url, URLPart.HOST), URLPart.WHOLE));
        return tokens;
    }


    private class Token {
        private final String token;
        private final URLPart part;

        public Token(String token, URLPart part) {
            this.token = token;
            this.part = part;
        }

        public String getToken() { return token; }

        public URLPart getPart() { return part; }
    }
}
