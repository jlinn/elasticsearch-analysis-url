package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.index.analysis.URLPart;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

/**
 * Joe Linn
 * 7/30/2015
 */
public class URLTokenizerTest extends BaseTokenStreamTestCase {
    public static final String TEST_HTTP_URL = "http://www.foo.bar.com:9200/index_name/type_name/_search.html?foo=bar&baz=bat#tag";
    public static final String TEST_HTTPS_URL = "https://www.foo.bar.com:9200/index_name/type_name/_search.html?foo=bar&baz=bat#tag";


    @Test
    public void testTokenizeProtocol() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PROTOCOL);
        assertTokenStreamContents(tokenizer, "http");

        tokenizer = createTokenizer(TEST_HTTPS_URL, URLPart.PROTOCOL);
        assertTokenStreamContents(tokenizer, "https");
    }


    @Test
    public void testTokenizeHost() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertTokenStreamContents(tokenizer, stringArray("www.foo.bar.com", "foo.bar.com", "bar.com", "com"));
    }


    @Test
    public void testTokenizePath() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertTokenStreamContents(tokenizer, stringArray("/index_name", "/index_name/type_name", "/index_name/type_name/_search.html"));

        tokenizer.reset();
        tokenizer.setReader(new StringReader(TEST_HTTPS_URL));
        tokenizer.setTokenizePath(false);

        assertTokenStreamContents(tokenizer, stringArray("/index_name/type_name/_search.html"));
    }


    @Test
    public void testTokenizeQuery() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.QUERY);
        assertTokenStreamContents(tokenizer, stringArray("foo=bar", "baz=bat"));
    }


    private URLTokenizer createTokenizer(String input, URLPart part) {
        return new URLTokenizer(new StringReader(input), part);
    }


    private String[] stringArray(String... strings) {
        return strings;
    }


    private static void assertTokenStreamContents(TokenStream in, String output) throws IOException {
        assertTokenStreamContents(in, new String[]{output});
    }
}