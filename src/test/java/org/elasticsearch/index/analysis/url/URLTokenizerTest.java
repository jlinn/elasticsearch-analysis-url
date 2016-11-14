package org.elasticsearch.index.analysis.url;

import com.google.common.collect.Lists;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.index.analysis.URLPart;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.analysis.url.IsTokenStreamWithTokenAndPosition.hasTokenAtOffset;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

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

        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PROTOCOL);
        assertThat(tokenizer, hasTokenAtOffset("http", 0, 4));

        tokenizer = createTokenizer(TEST_HTTPS_URL, URLPart.PROTOCOL);
        assertTokenStreamContents(tokenizer, "https");
    }


    @Test
    public void testTokenizeHost() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertTokenStreamContents(tokenizer, stringArray("www.foo.bar.com", "foo.bar.com", "bar.com", "com"));

        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertThat(tokenizer, hasTokenAtOffset("www.foo.bar.com", 7, 22));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertThat(tokenizer, hasTokenAtOffset("foo.bar.com", 11, 22));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertThat(tokenizer, hasTokenAtOffset("bar.com", 15, 22));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertThat(tokenizer, hasTokenAtOffset("com", 19, 22));
    }


    @Test
    public void testTokenizePort() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PORT);
        assertThat(tokenizer, hasTokenAtOffset("9200", 23, 27));

        tokenizer = createTokenizer("http://foo.bar.com", URLPart.PORT);
        assertThat(tokenizer, hasTokenAtOffset("80", 0, 0));
    }


    @Test
    public void testTokenizePath() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertTokenStreamContents(tokenizer, stringArray("/index_name", "/index_name/type_name", "/index_name/type_name/_search.html"));

        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertThat(tokenizer, hasTokenAtOffset("/index_name", 27, 38));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertThat(tokenizer, hasTokenAtOffset("/index_name/type_name", 27, 48));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertThat(tokenizer, hasTokenAtOffset("/index_name/type_name/_search.html", 27, 61));

        tokenizer.reset();
        tokenizer.setReader(new StringReader(TEST_HTTPS_URL));
        tokenizer.setTokenizePath(false);

        assertTokenStreamContents(tokenizer, stringArray("/index_name/type_name/_search.html"));
    }


    @Test
    public void testTokenizeNoPath() throws Exception {
        final String url = "http://www.foo.bar.com:9200";
        URLTokenizer tokenizer = createTokenizer(url, URLPart.PATH);
        assertTokenStreamContents(tokenizer, stringArray());
    }


    @Test
    public void testTokenizeQuery() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.QUERY);
        assertTokenStreamContents(tokenizer, stringArray("foo=bar", "baz=bat"));

        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.QUERY);
        assertThat(tokenizer, hasTokenAtOffset("foo=bar", 62, 69));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.QUERY);
        assertThat(tokenizer, hasTokenAtOffset("baz=bat", 70, 77));
    }


    @Test
    public void testTokenizeRef() throws IOException {
        URLTokenizer tokenizer = createTokenizer("http://foo.com#baz", URLPart.REF);
        assertThat(tokenizer, hasTokenAtOffset("baz", 15, 18));
    }


    @Test
    public void testAll() throws IOException {
        URLTokenizer tokenizer = new URLTokenizer();
        tokenizer.setReader(new StringReader(TEST_HTTPS_URL));
        CharTermAttribute termAttribute = tokenizer.getAttribute(CharTermAttribute.class);
        tokenizer.reset();
        tokenizer.clearAttributes();
        List<String> tokens = new ArrayList<>();
        while(tokenizer.incrementToken()){
            tokens.add(termAttribute.toString());
        }

        assertThat(tokens, hasItem(equalTo("https")));
        assertThat(tokens, hasItem(equalTo("foo.bar.com")));
        assertThat(tokens, hasItem(equalTo("www.foo.bar.com:9200")));
        assertThat(tokens, hasItem(equalTo("https://www.foo.bar.com")));

        tokenizer = createTokenizer("https://foo.com", null);
        assertThat(tokenizer, hasTokenAtOffset("https", 0, 5));
    }


    @Test(expected = IOException.class)
    public void testMalformed() throws IOException {
        URLTokenizer tokenizer = createTokenizer("://foo.com", URLPart.QUERY);
        assertTokenStreamContents(tokenizer, stringArray("foo=bar", "baz=bat"));
    }


    @Test
    public void testAllowMalformed() throws IOException {
        URLTokenizer tokenizer = createTokenizer("://foo.com", URLPart.QUERY);
        tokenizer.setAllowMalformed(true);
        assertTokenStreamContents(tokenizer, stringArray("://foo.com"));
    }


    @Test
    public void testUrlDecode() throws Exception {
        String url = "http://foo.com?baz=foo%20bat";
        URLTokenizer tokenizer = createTokenizer(url, URLPart.QUERY);
        tokenizer.setUrlDecode(true);
        assertTokenStreamContents(tokenizer, stringArray("baz=foo bat"));
    }


    @Test(expected = IOException.class)
    public void testUrlDecodeIllegalCharacters() throws Exception {
        String url = "http://foo.com?baz=foo%2vbat";
        URLTokenizer tokenizer = createTokenizer(url, URLPart.QUERY);
        tokenizer.setUrlDecode(true);
        assertTokenStreamContents(tokenizer, "");
    }


    @Test
    public void testUrlDecodeAllowMalformed() throws Exception {
        String url = "http://foo.com?baz=foo%2vbat";
        URLTokenizer tokenizer = createTokenizer(url, URLPart.QUERY);
        tokenizer.setUrlDecode(true);
        tokenizer.setAllowMalformed(true);
        assertTokenStreamContents(tokenizer, "baz=foo%2vbat");
    }


    @Test
    public void testPartialUrl() throws Exception {
        final String url = "http://";
        URLTokenizer tokenizer = createTokenizer(url, URLPart.QUERY);
        assertTokenStreamContents(tokenizer, new String[]{});
    }


    @Test
    public void testNoProtocol() throws Exception {
        final String url = "foo.bar.baz/bat/blah.html";
        URLTokenizer tokenizer = createTokenizer(url, URLPart.PATH);
        tokenizer.setAllowMalformed(true);
        tokenizer.setTokenizeMalformed(true);
        assertTokenStreamContents(tokenizer, stringArray("/bat", "/bat/blah.html"));
    }


    @Test
    public void testMalformedGetRef() throws Exception {
        String url = "/bat/blah.html#tag?baz=bat";
        URLTokenizer tokenizer = createTokenizer(url, URLPart.REF);
        tokenizer.setAllowMalformed(true);
        tokenizer.setTokenizeMalformed(true);
        assertTokenStreamContents(tokenizer, stringArray("tag"));
    }


    @Test
    public void testMalformedWhole() throws Exception {
        String url = "foo.bar.com/baz.html/query?a=1";
        URLTokenizer tokenizer = createTokenizer(url, URLPart.WHOLE);
        tokenizer.setAllowMalformed(true);
        tokenizer.setTokenizeMalformed(true);
        assertTokenStreamContents(tokenizer, stringArray("foo.bar.com/baz.html/query?a=1"));
    }


    @Test
    public void testProtocolAndPort() throws Exception {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PROTOCOL, URLPart.PORT);
        assertTokenStreamContents(tokenizer, stringArray("http", "9200"));
    }


    @Test
    public void testMalformedHostAndWhole() throws Exception {
        URLTokenizer tokenizer = createTokenizer("example.com", URLPart.WHOLE, URLPart.HOST);
        tokenizer.setAllowMalformed(true);
        tokenizer.setTokenizeMalformed(true);
        tokenizer.setTokenizeHost(false);
        assertTokenStreamContents(tokenizer, stringArray("example.com"));
    }


    @Test
    public void testTokenizeMalformedNoPartSpecified() throws Exception {
        URLTokenizer tokenizer = createTokenizer("example.com");
        tokenizer.setAllowMalformed(true);
        tokenizer.setTokenizeMalformed(true);
        tokenizer.setTokenizeHost(false);
        assertTokenStreamContents(tokenizer, stringArray("example.com"));
    }


    @Test
    public void testAllowMalformedNoPartsSpecified() throws Exception {
        URLTokenizer tokenizer = createTokenizer("example.com");
        tokenizer.setAllowMalformed(true);
        tokenizer.setTokenizeHost(false);
        assertTokenStreamContents(tokenizer, stringArray("example.com"));
    }


    @Test
    public void testTokenizeSpecial() throws Exception {
        final String url = "http://www.foo.bar.com:8080/baz/bat?bob=blah";
        URLTokenizer tokenizer = createEverythingTokenizer(url);
        assertThat(tokenizer, hasTokenAtOffset("www.foo.bar.com:8080", 7, 27));
        tokenizer = createEverythingTokenizer(url);
        assertThat(tokenizer, hasTokenAtOffset("www.foo.bar.com", 7, 22));
        tokenizer = createEverythingTokenizer(url);
        assertThat(tokenizer, hasTokenAtOffset("foo.bar.com", 11, 22));
        tokenizer = createEverythingTokenizer(url);
        assertThat(tokenizer, hasTokenAtOffset("bar.com", 15, 22));
    }


    private URLTokenizer createEverythingTokenizer(String input) throws IOException {
        URLTokenizer tokenizer = createTokenizer(input);
        tokenizer.setAllowMalformed(true);
        tokenizer.setUrlDecode(true);
        tokenizer.setTokenizeMalformed(true);
        tokenizer.setTokenizeHost(true);
        tokenizer.setTokenizePath(true);
        tokenizer.setTokenizeQuery(true);
        return tokenizer;
    }


    private URLTokenizer createTokenizer(String input, URLPart... parts) throws IOException {
        URLTokenizer tokenizer = new URLTokenizer();
        if (parts != null) {
            tokenizer.setParts(Lists.newArrayList(parts));
        }
        tokenizer.setReader(new StringReader(input));
        return tokenizer;
    }


    private String[] stringArray(String... strings) {
        return strings;
    }


    private static void assertTokenStreamContents(TokenStream in, String output) throws IOException {
        assertTokenStreamContents(in, new String[]{output});
    }
}