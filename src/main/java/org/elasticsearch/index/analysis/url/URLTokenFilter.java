package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.path.PathHierarchyTokenizer;
import org.apache.lucene.analysis.path.ReversePathHierarchyTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.analysis.URLPart;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Joe Linn
 * 1/17/2015
 */
public final class URLTokenFilter extends TokenFilter {
    public static final String NAME = "url";

    private List<URLPart> parts;

    private boolean urlDeocde;

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

    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
    private final OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);

    private final boolean allowMalformed;

    private boolean tokenizeMalformed;

    private boolean passthrough;

    private List<Token> tokens;
    private Iterator<Token> iterator;

    public URLTokenFilter(TokenStream input, URLPart part) {
        this(input, part, false);
    }

    public URLTokenFilter(TokenStream input, URLPart part, boolean urlDecode) {
        this(input, part, urlDecode, false);
    }

    public URLTokenFilter(TokenStream input, URLPart part, boolean urlDecode, boolean allowMalformed) {
        this(input, part, urlDecode, allowMalformed, false);
    }

    public URLTokenFilter(TokenStream input, URLPart part, boolean urlDecode, boolean allowMalformed, boolean passthrough) {
        super(input);
        if (part != null) {
            this.parts = Collections.singletonList(part);
        } else {
            parts = null;
        }
        this.urlDeocde = urlDecode;
        this.allowMalformed = allowMalformed;
        this.passthrough = passthrough;
    }


    public URLTokenFilter setParts(List<URLPart> parts) {
        this.parts = parts;
        return this;
    }

    public URLTokenFilter setTokenizeHost(boolean tokenizeHost) {
        this.tokenizeHost = tokenizeHost;
        return this;
    }

    public URLTokenFilter setTokenizePath(boolean tokenizePath) {
        this.tokenizePath = tokenizePath;
        return this;
    }

    public URLTokenFilter setTokenizeQuery(boolean tokenizeQuery) {
        this.tokenizeQuery = tokenizeQuery;
        return this;
    }


    public URLTokenFilter setTokenizeMalformed(boolean tokenizeMalformed) {
        this.tokenizeMalformed = tokenizeMalformed;
        return this;
    }

    public URLTokenFilter setUrlDeocde(boolean urlDeocde) {
        this.urlDeocde = urlDeocde;
        return this;
    }


    @Override
    public boolean incrementToken() throws IOException {
        if (iterator == null || !iterator.hasNext()) {
            if ((iterator != null && !iterator.hasNext() && !passthrough) || !advance()) {
                return false;
            }
        }
        clearAttributes();
        Token next = iterator.next();
        termAttribute.append(next.getToken());
        typeAttribute.setType(next.getPart().name().toLowerCase());
        offsetAttribute.setOffset(next.getStart(), next.getEnd());
        return true;
    }


    /**
     * Advance to the next token, if any
     * @return true if more tokens are forthcoming, false otherwise
     * @throws IOException
     */
    private boolean advance() throws IOException {
        if (input.incrementToken()) {
            String urlString = termAttribute.toString();
            if ((Strings.isNullOrEmpty(urlString) || "null".equals(urlString)) && !allowMalformed && !passthrough) {
                return false;
            }
            try {
                tokens = tokenize(urlString);
            } catch (IOException e) {
                if (e.getMessage().contains("Malformed URL")) {
                    if (allowMalformed) {
                        tokens = Collections.singletonList(new Token(urlString, URLPart.WHOLE, 0, urlString.length()));
                    } else {
                        throw new MalformedURLException("Malformed URL: " + urlString);
                    }
                }
                throw e;
            }
            if (tokens.isEmpty()) {
                return false;
            }
            iterator = tokens.iterator();
            return true;
        } else {
            return false;
        }
    }


    /**
     * Tokenize the given input using a {@link URLTokenizer}. Settings which have been set on this {@link URLTokenFilter}
     * will be passed along to the tokenizer.
     * @param input a string to be tokenized
     * @return a list of tokens extracted from the input string
     * @throws IOException
     */
    private List<Token> tokenize(String input) throws IOException {
        List<Token> tokens = new ArrayList<>();
        URLTokenizer tokenizer = new URLTokenizer();
        // create a copy of the parts list to avoid ConcurrentModificationException when sorting
        tokenizer.setParts(new ArrayList<>(parts));
        tokenizer.setUrlDecode(urlDeocde);
        tokenizer.setTokenizeHost(tokenizeHost);
        tokenizer.setTokenizePath(tokenizePath);
        tokenizer.setTokenizeQuery(tokenizeQuery);
        tokenizer.setAllowMalformed(allowMalformed || passthrough);
        tokenizer.setTokenizeMalformed(tokenizeMalformed);
        tokenizer.setReader(new StringReader(input));
        tokenizer.reset();

        String term;
        URLPart part;
        OffsetAttribute offset;
        while (tokenizer.incrementToken()) {
            term = tokenizer.getAttribute(CharTermAttribute.class).toString();
            part = URLPart.fromString(tokenizer.getAttribute(TypeAttribute.class).type());
            offset = tokenizer.getAttribute(OffsetAttribute.class);
            tokens.add(new Token(term, part, offset.startOffset(), offset.endOffset()));
        }
        return tokens;
    }


    @Override
    public void reset() throws IOException {
        super.reset();
        tokens = null;
        iterator = null;
    }

    private static final Pattern REGEX_PROTOCOL = Pattern.compile("^([a-zA-Z]+)(?=://)");
    private static final Pattern REGEX_PORT = Pattern.compile(":([0-9]{1,5})");
    private static final Pattern REGEX_QUERY = Pattern.compile("\\?(.+)");

    /**
     * Attempt to parse a malformed url string
     * @param urlString the malformed url string
     * @return the url part if it can be parsed, null otherwise
     * @deprecated parsing of malformed URLs is now delegated to {@link URLTokenizer}
     */
    private String parseMalformed(String urlString) {
        if (parts != null && !parts.isEmpty()) {
            String ret;
            for (URLPart part : parts) {
                switch (part) {
                    case PROTOCOL:
                        ret = applyPattern(REGEX_PROTOCOL, urlString);
                        break;
                    case PORT:
                        ret = applyPattern(REGEX_PORT, urlString);
                        break;
                    case QUERY:
                        ret = applyPattern(REGEX_QUERY, urlString);
                        break;
                    case WHOLE:
                        ret = urlString;
                        break;
                    default:
                        ret = urlString;
                }
                if (!Strings.isNullOrEmpty(ret)) {
                    return ret;
                }
            }
        }
        return urlString;
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
}
