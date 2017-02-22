package org.elasticsearch.index.analysis.url;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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
import org.elasticsearch.index.analysis.URLPartComparator;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

import static org.elasticsearch.index.analysis.url.URLUtils.getPart;

/**
 * Joe Linn
 * 7/30/2015
 */
public final class URLTokenizer extends Tokenizer {
    private static final URLPartComparator PART_COMPARATOR = new URLPartComparator();

    /**
     * If set, only the given part of the url will be tokenized.
     */
    private List<URLPart> parts;

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

    /**
     * Has no effect if {@link #allowMalformed} is false. If both are true, an attempt will be made to tokenize malformed
     * URLs using regular expressions.
     */
    private boolean tokenizeMalformed;


    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
    private final OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);

    private List<Token> tokens;
    private Iterator<Token> iterator;


    public URLTokenizer() {

    }

    public URLTokenizer(URLPart part) {
        setPart(part);
    }


    public URLTokenizer(AttributeFactory factory) {
        super(factory);
    }

    public void setParts(List<URLPart> parts) {
        if (parts != null) {
            parts.sort(PART_COMPARATOR);
            this.parts = parts;
        }
    }

    public void setPart(URLPart part) {
        if (part != null) {
            this.parts = Collections.singletonList(part);
        }
    }

    public void setUrlDecode(boolean urlDecode) { this.urlDecode = urlDecode; }

    public void setTokenizeHost(boolean tokenizeHost) { this.tokenizeHost = tokenizeHost; }

    public void setTokenizePath(boolean tokenizePath) { this.tokenizePath = tokenizePath; }

    public void setTokenizeQuery(boolean tokenizeQuery) { this.tokenizeQuery = tokenizeQuery; }

    public void setAllowMalformed(boolean allowMalformed) { this.allowMalformed = allowMalformed; }

    public void setTokenizeMalformed(boolean tokenizeMalformed) { this.tokenizeMalformed = tokenizeMalformed; }

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
            if (parts != null && !parts.isEmpty()) {
                List<Token> tokensList = new ArrayList<>();
                for (URLPart part : parts) {
                    tokensList.addAll(tokenize(url, part));
                }
                return tokensList;
            }
            // No part is specified. Tokenize all parts.
            Set<Token> tokens = new LinkedHashSet<>();
            for (URLPart urlPart : URLPart.values()) {
                tokens.addAll(tokenize(url, urlPart));
            }
            tokens.addAll(tokenizeSpecial(url));
            return Lists.newArrayList(tokens);
        } catch (MalformedURLException e) {
            if (allowMalformed) {
                if (tokenizeMalformed && parts != null && !parts.isEmpty()) {
                    return tokenizePartsMalformed(urlString, parts);
                }
                return tokenizeMalformed(urlString, (parts == null || parts.isEmpty()) ? null : URLPart.WHOLE);
            }
            throw new IOException("Malformed URL: " + urlString, e);
        }
    }


    /**
     * Tokenize all given parts of the given URL while ensuring that duplicate tokens are not created when the whole
     * malformed URL is is identical to a single part token.
     * @param urlString the malformed URL to be tokenized
     * @param parts the desired {@link URLPart}s in proper part order
     * @return a list of {@link Token}s
     * @throws IOException
     */
    private List<Token> tokenizePartsMalformed(String urlString, List<URLPart> parts) throws IOException {
        List<Token> tokens = new ArrayList<>();
        Set<String> tokenStrings = new HashSet<>();
        for (URLPart part : parts) {
            for (Token token : tokenizeMalformed(urlString, part)) {
                if (part != URLPart.WHOLE) {
                    tokens.add(token);
                    tokenStrings.add(token.getToken());
                } else if (tokenStrings.isEmpty()) {
                    // If we couldn't tokenize any of the parts, add the whole thing.
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }


    /**
     * Attempt to tokenize the given malformed URL.
     * @param url the URL to be tokenized
     * @param part the desired part of the URL
     * @return {@link List} of {@link Token}s gleaned from the given URL
     * @throws IOException
     */
    private List<Token> tokenizeMalformed(String url, URLPart part) throws IOException {
        if (part == null) {
            // No part is specified. Tokenize all parts.
            List<URLPart> urlParts = Arrays.asList(URLPart.values());
            urlParts.sort(new URLPartComparator());
            return tokenizePartsMalformed(url, urlParts);
        }
        Optional<String> partOptional = getPart(url, part);
        if (!partOptional.isPresent() || partOptional.get().equals("")) {
            // desired part was not found
            return new ArrayList<>();
        }
        final String partStringRaw = partOptional.get();
        int start = 0;
        int end = 0;
        String partString = urlDecode(partOptional.get());
        switch (part) {
            case HOST:
                return getHostTokens(url, partStringRaw, partString);
            case PORT:
                return getPortTokens(url, partStringRaw);
            case PATH:
                return getPathTokens(url, partStringRaw, partString);
            case REF:
                return getRefTokens(url, partStringRaw, partString);
            case QUERY:
                return getQueryTokens(url, partStringRaw, partString);
            case PROTOCOL:
                return Collections.singletonList(new Token(partString, part, start, partString.length()));
            case WHOLE:
                return Collections.singletonList(new Token(url, URLPart.WHOLE, 0, url.length() - 1));
            default:
        }
        return Collections.singletonList(new Token(partString, part, start, end));
    }


    /**
     * URL decode the given string if {@link #urlDecode} is true. The given <code>partString</code> is passed through
     * unaltered otherwise.
     * @param partString string to be URL decoded
     * @return URL decoded string if {@link #urlDecode} is true; unaltered string otherwise.
     * @throws IOException if malformed URL encoding is present and {@link #allowMalformed} is false.
     */
    private String urlDecode(String partString) throws IOException {
        if (urlDecode) {
            try {
                partString = URLDecoder.decode(partString, "UTF-8");
            } catch (IllegalArgumentException e) {
                if (!allowMalformed) {
                    throw new IOException("Error performing URL decoding on string: " + partString, e);
                }
            }
        }
        return partString;
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
        partString = urlDecode(partString);
        switch (part) {
            case HOST:
                return getHostTokens(url, partStringRaw, partString);
            case PORT:
                return getPortTokens(url, getPart(url, part));
            case PATH:
                return getPathTokens(url, partStringRaw, partString);
            case QUERY:
                return getQueryTokens(url, partStringRaw, partString);
            case PROTOCOL:
            case WHOLE:
                end = partString.length();
                break;
            case REF:
                return getRefTokens(url, partStringRaw, partString);
            default:
        }
        return Collections.singletonList(new Token(partString, part, start, end));
    }


    /**
     * Retrieve tokens representing the host of the given URL
     * @param url URL to be tokenized
     * @param partStringRaw raw (not url decoded) string containing the host
     * @param partString potentially url decoded string containing the host
     * @return host tokens
     * @throws IOException
     */
    private List<Token> getHostTokens(URL url, String partStringRaw, String partString) throws IOException {
        return getHostTokens(url.toString(), partStringRaw, partString);
    }


    /**
     * Retrieve tokens representing the host of the given URL
     * @param url URL to be tokenized
     * @param partStringRaw raw (not url decoded) string containing the host
     * @param partString potentially url decoded string containing the host
     * @return host tokens
     * @throws IOException
     */
    private List<Token> getHostTokens(String url, String partStringRaw, String partString) throws IOException {
        int start = getStartIndex(url, partStringRaw);
        if (!tokenizeHost || InetAddresses.isInetAddress(partString)) {
            int end = getEndIndex(start, partStringRaw);
            return Collections.singletonList(new Token(partString, URLPart.HOST, start, end));
        }
        return tokenize(URLPart.HOST, addReader(new ReversePathHierarchyTokenizer('.', '.'), new StringReader(partString)), start);
    }


    private List<Token> getPortTokens(URL url, String port) {
        return getPortTokens(url.toString(), port);
    }


    private List<Token> getPortTokens(String url, String port) {
        int start = url.indexOf(":" + port);
        int end = 0;
        if (start == -1) {
            // port was inferred
            start = 0;
        } else {
            // explicit port
            start++;    // account for :
            end = getEndIndex(start, port);
        }
        return Collections.singletonList(new Token(port, URLPart.PORT, start, end));
    }


    private List<Token> getPathTokens(URL url, String partStringRaw, String partString) throws IOException {
        return getPathTokens(url.toString(), partStringRaw, partString);
    }


    private List<Token> getPathTokens(String url, String partStringRaw, String partString) throws IOException {
        int start = getStartIndex(url, partStringRaw);
        if (!tokenizePath) {
            int end = getEndIndex(start, partStringRaw);
            return Collections.singletonList(new Token(partString, URLPart.PATH, start, end));
        }
        return tokenize(URLPart.PATH, addReader(new PathHierarchyTokenizer('/', '/'), new StringReader(partString)), start);
    }


    private List<Token> getRefTokens(URL url, String partStringRaw, String partString) {
        return getRefTokens(url.toString(), partStringRaw, partString);
    }


    private List<Token> getRefTokens(String url, String partStringRaw, String partString) {
        int start = getStartIndex(url, "#" + partStringRaw) + 1;
        int end = url.length();
        return Collections.singletonList(new Token(partString, URLPart.REF, start, end));
    }


    private List<Token> getQueryTokens(URL url, String partStringRaw, String partString) throws IOException {
        return getQueryTokens(url.toString(), partStringRaw, partString);
    }


    private List<Token> getQueryTokens(String url, String partStringRaw, String partString) throws IOException {
        int start = getStartIndex(url, partStringRaw);
        if (!tokenizeQuery) {
            int end = getEndIndex(start, partStringRaw);
            return Collections.singletonList(new Token(partString, URLPart.QUERY, start, end));
        }
        return tokenize(URLPart.QUERY, addReader(new PatternTokenizer(QUERY_SEPARATOR, -1), new StringReader(partString)), start);
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
        return getStartIndex(url.toString(), partStringRaw);
    }


    private int getStartIndex(String url, String partStringRaw) {
        return url.indexOf(partStringRaw);
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
    private List<Token> tokenize(URLPart part, Tokenizer tokenizer, int start) throws IOException {
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
    private List<Token> tokenizeSpecial(URL url) {
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


}
