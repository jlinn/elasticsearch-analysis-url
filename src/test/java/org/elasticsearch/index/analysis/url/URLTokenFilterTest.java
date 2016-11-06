package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.CannedTokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.index.analysis.URLPart;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.elasticsearch.index.analysis.url.IsTokenStreamWithTokenAndPosition.hasTokenAtOffset;

public class URLTokenFilterTest extends BaseTokenStreamTestCase {
    public static final String TEST_HTTP_URL = "http://www.foo.bar.com:9200/index_name/type_name/_search.html?foo=bar&baz=bat#tag";
    public static final String TEST_HTTP_URL2 = "http://www.foo.bar.com";
    public static final String TEST_HTTPS_URL = "https://www.foo.bar.com:9200/index_name/type_name/_search.html?foo=bar&baz=bat#tag";

    @Test
    public void testFilterProtocol() throws IOException {
        URLTokenFilter filter = createFilter(TEST_HTTP_URL, URLPart.PROTOCOL);
        assertTokenStreamContents(filter, "http");

        filter = createFilter(TEST_HTTPS_URL, URLPart.PROTOCOL);
        assertTokenStreamContents(filter, "https");
    }

    @Test
    public void testFilterHost() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.HOST).setTokenizeHost(false), "www.foo.bar.com");

        URLTokenFilter filter = createFilter(TEST_HTTP_URL, URLPart.HOST)
                .setUrlDeocde(false);
        assertThat(filter, hasTokenAtOffset("www.foo.bar.com", 7, 22));
        filter = createFilter(TEST_HTTP_URL, URLPart.HOST)
                .setUrlDeocde(false);
        assertThat(filter, hasTokenAtOffset("foo.bar.com", 11, 22));
        filter = createFilter(TEST_HTTP_URL, URLPart.HOST)
                .setUrlDeocde(false);
        assertThat(filter, hasTokenAtOffset("bar.com", 15, 22));
        filter = createFilter(TEST_HTTP_URL, URLPart.HOST)
                .setUrlDeocde(false);
        assertThat(filter, hasTokenAtOffset("com", 19, 22));
    }

    @Test
    public void testFilterPort() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.PORT), "9200");
    }

    @Test
    public void testFilterPath() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.PATH).setTokenizePath(false), "/index_name/type_name/_search.html");
        assertTokenStreamContents(createFilter(TEST_HTTP_URL2, URLPart.PATH).setTokenizePath(false), new String[]{});
    }

    @Test
    public void testFilterRef() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.REF), "tag");
    }

    @Test
    public void testFilterQuery() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.QUERY).setTokenizeQuery(false), "foo=bar&baz=bat");
    }

    @Test(expected = MalformedURLException.class)
    public void testInvalidURL() throws IOException {
        URLTokenFilter filter = createFilter("foobar", URLPart.HOST);
        filter.incrementToken();
    }

    @Test
    public void testNullURL() throws IOException {
        URLTokenFilter filter = createFilter(null, URLPart.HOST);
        filter.incrementToken();
    }

    @Test
    public void testUrlDecode() throws IOException {
        assertTokenStreamContents(createFilter("https://www.foo.com?email=foo%40bar.com", URLPart.QUERY, true), "email=foo@bar.com");
    }

    @Test
    public void testInferPort() throws IOException {
        assertTokenStreamContents(createFilter("http://www.foo.bar.com/baz/bat.html", URLPart.PORT), "80");
        assertTokenStreamContents(createFilter("https://www.foo.bar.com/baz/bat.html", URLPart.PORT), "443");
        assertTokenStreamContents(createFilter("https://foo.bar.com", URLPart.PORT), "443");
    }

    @Test
    public void testMalformed() throws IOException {
        URLTokenFilter filter = createFilter("http://:::::::/baz", URLPart.PROTOCOL, false, true);
        filter.setTokenizeMalformed(true);
        assertTokenStreamContents(filter, "http");

        filter = createFilter("foo.com/bar?baz=bat", URLPart.QUERY, false, true);
        filter.setTokenizeMalformed(true);
        assertTokenStreamContents(filter, "baz=bat");

        filter = createFilter("baz.com:3456/foo", URLPart.PORT, false, true);
        filter.setTokenizeMalformed(true);
        assertTokenStreamContents(filter, "3456");
    }

    private URLTokenFilter createFilter(final String url, final URLPart part) {
        return createFilter(url, part, false);
    }

    private URLTokenFilter createFilter(final String url, final URLPart part, final boolean urlDecode) {
        return createFilter(url, part, urlDecode, false);
    }

    private URLTokenFilter createFilter(final String url, final URLPart part, final boolean urlDecode, final boolean allowMalformed) {
        int length = 0;
        if (url != null) {
            length = url.length();
        }
        return new URLTokenFilter(new CannedTokenStream(new Token(url, 0, length)), part, urlDecode, allowMalformed);
    }

    private static void assertTokenStreamContents(TokenStream in, String output) throws IOException {
        assertTokenStreamContents(in, new String[]{output});
    }
}