package org.elasticsearch.index.analysis.url;

import com.google.common.collect.ImmutableList;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.path.PathHierarchyTokenizer;
import org.apache.lucene.analysis.path.ReversePathHierarchyTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.analysis.URLPart;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
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

    private final URLPart part;

    private final boolean urlDeocde;

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

    private final boolean allowMalformed;

    private boolean tokenizeMalformed;

    private boolean passthrough;

    private List<String> tokens;
    private Iterator<String> iterator;

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
        this.part = part;
        this.urlDeocde = urlDecode;
        this.allowMalformed = allowMalformed;
        this.passthrough = passthrough;
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

    @Override
    public boolean incrementToken() throws IOException {
        if(iterator == null || !iterator.hasNext()){
            if ((iterator != null && !iterator.hasNext() && !passthrough) || !advance()) {
                return false;
            }
        }
        clearAttributes();
        String next = iterator.next();
        if (allowMalformed) {
            next = parseMalformed(next);
        }
        termAttribute.append(next);
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
                        tokens = ImmutableList.of(urlString);
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
    private List<String> tokenize(String input) throws IOException {
        List<String> tokens = new ArrayList<>();
        URLTokenizer tokenizer = new URLTokenizer(part);
        tokenizer.setUrlDecode(urlDeocde);
        tokenizer.setTokenizeHost(tokenizeHost);
        tokenizer.setTokenizePath(tokenizePath);
        tokenizer.setTokenizeQuery(tokenizeQuery);
        tokenizer.setAllowMalformed(allowMalformed || passthrough);
        tokenizer.setTokenizeMalformed(tokenizeMalformed);
        tokenizer.setReader(new StringReader(input));
        tokenizer.reset();
        while (tokenizer.incrementToken()) {
            tokens.add(tokenizer.getAttribute(CharTermAttribute.class).toString());
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
                return urlString;
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
}
