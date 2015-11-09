package org.elasticsearch.index.analysis.url;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.path.PathHierarchyTokenizer;
import org.apache.lucene.analysis.path.ReversePathHierarchyTokenizer;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
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
     * If true, the url's query string will be split on &
     */
    private boolean tokenizeQuery = true;

    /**
     * If true, {@link MalformedURLException} will be suppressed, and the given string will be returned as a single token
     */
    private boolean allowMalformed;


    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
    private final OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);

    private List<Token> tokens;
    private Iterator<Token> iterator;


    public URLTokenizer() {

    }

    public URLTokenizer(URLPart part) {
        this.part = part;
    }


    public URLTokenizer(AttributeFactory factory) {
        super(factory);
    }


    public void setPart(URLPart part) { this.part = part; }

    public void setUrlDecode(boolean urlDecode) { this.urlDecode = urlDecode; }

    public void setTokenizeHost(boolean tokenizeHost) { this.tokenizeHost = tokenizeHost; }

    public void setTokenizePath(boolean tokenizePath) { this.tokenizePath = tokenizePath; }

    public void setTokenizeQuery(boolean tokenizeQuery) { this.tokenizeQuery = tokenizeQuery; }

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
        offsetAttribute.setOffset(token.getStart(), token.getEnd());
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
                return ImmutableList.of(new Token(urlString, URLPart.WHOLE, 0, urlString.length() - 1));
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
        if (Strings.isNullOrEmpty(partString)) {
            // desired part was not found
            return new ArrayList<>();
        }
        final String partStringRaw = partString;
        int start = 0;
        int end = 0;
        if (urlDecode) {
            partString = URLDecoder.decode(partString, "UTF-8");
        }
        switch (part) {
            case HOST:
                start = getStartIndex(url, partStringRaw);
                if (!tokenizeHost || InetAddresses.isInetAddress(partString)) {
                    end = getEndIndex(start, partStringRaw);
                    return ImmutableList.of(new Token(partString, part, start, end));
                }
                return tokenize(part, addReader(new ReversePathHierarchyTokenizer('.', '.'), new StringReader(partString)), start);
            case PORT:
                String port = getPort(url);
                start = url.toString().indexOf(":" + port);
                if (start == -1) {
                    // port was inferred
                    start = 0;
                } else {
                    // explicit port
                    start++;    // account for :
                    end = getEndIndex(start, port);
                }
                return ImmutableList.of(new Token(port, part, start, end));
            case PATH:
                start = getStartIndex(url, partStringRaw);
                if (!tokenizePath) {
                    end = getEndIndex(start, partStringRaw);
                    return ImmutableList.of(new Token(partString, part, start, end));
                }
                return tokenize(part, addReader(new PathHierarchyTokenizer('/', '/'), new StringReader(partString)), start);
            case QUERY:
                start = getStartIndex(url, partStringRaw);
                if (!tokenizeQuery) {
                    end = getEndIndex(start, partStringRaw);
                    return ImmutableList.of(new Token(partString, part, start, end));
                }
                return tokenize(part, addReader(new PatternTokenizer(QUERY_SEPARATOR, -1), new StringReader(partString)), start);
            case PROTOCOL:
            case WHOLE:
                end = partString.length();
                break;
            case REF:
                start = getStartIndex(url, "#" + partStringRaw) + 1;
                end = url.toString().length();
            default:
        }
        return ImmutableList.of(new Token(partString, part, start, end));
    }


    /**
     * Set the given reader on the given tokenizer
     * @param tokenizer tokenizer on which the reader is to be set
     * @param input the reader to set
     * @return the given tokenizer with the given reader set
     * @throws IOException
     */
    private Tokenizer addReader(Tokenizer tokenizer, Reader input) throws IOException {
        tokenizer.setReader(input);
        return tokenizer;
    }


    /**
     * Get the start index of the given string in the given url
     * @param url the url
     * @param partStringRaw the url part
     * @return the starting index of the part string if it is found in the given url, -1 if it is not found
     */
    private int getStartIndex(URL url, String partStringRaw) {
        return url.toString().indexOf(partStringRaw);
    }


    /**
     * Get the end index of the given part string
     * @param start the start index of the part string
     * @param partStringRaw the part string
     * @return the end index
     */
    private int getEndIndex(int start, String partStringRaw) {
        return start + partStringRaw.length();
    }


    /**
     * Get a list of {@link Token}s from the given {@link Tokenizer}
     * @param part the url part which should be used in {@link Token} creation
     * @param tokenizer the tokenizer from which tokens will be gleaned
     * @return a list of tokens
     * @throws IOException
     */
    List<Token> tokenize(URLPart part, Tokenizer tokenizer, int start) throws IOException {
        tokenizer.reset();
        List<Token> tokens = new ArrayList<>();
        OffsetAttribute offset;
        String token;
        while (tokenizer.incrementToken()) {
            token = tokenizer.getAttribute(CharTermAttribute.class).toString();
            offset = tokenizer.getAttribute(OffsetAttribute.class);
            tokens.add(new Token(token, part, start + offset.startOffset(), start + offset.endOffset()));
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
        String token = getPart(url, URLPart.HOST) + ":" + getPart(url, URLPart.PORT);
        int start = getStartIndex(url, token);
        int end = 0;
        if(start == -1){
            // implicit port
            start = 0;
        } else {
            end = getEndIndex(start, token);
        }
        tokens.add(new Token(token, URLPart.WHOLE, start, end));

        // protocol://host
        token = getPart(url, URLPart.PROTOCOL) + "://" + getPart(url, URLPart.HOST);
        start = getStartIndex(url, token);
        end = getEndIndex(start, token);
        tokens.add(new Token(token, URLPart.WHOLE, start, end));
        return tokens;
    }


    private class Token {
        private final String token;
        private final URLPart part;
        private final int start;
        private final int end;

        public Token(String token, URLPart part, int start, int end) {
            this.token = token;
            this.part = part;
            this.start = start;
            this.end = end;
        }

        public String getToken() { return token; }

        public URLPart getPart() { return part; }

        public int getStart() { return start; }

        public int getEnd() { return end; }
    }
}
