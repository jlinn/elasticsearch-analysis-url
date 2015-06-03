package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.SingleTokenTokenStream;
import org.elasticsearch.index.analysis.URLPart;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

public class URLTokenFilterTest extends BaseTokenStreamTestCase {
    public static final String TEST_HTTP_URL = "http://www.foo.bar.com:9200/index_name/type_name/_search.html?foo=bar&baz=bat#tag";
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
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.HOST), "www.foo.bar.com");
    }

    @Test
    public void testFilterPort() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.PORT), "9200");
    }

    @Test
    public void testFilterPath() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.PATH), "/index_name/type_name/_search.html");
    }

    @Test
    public void testFilterRef() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.REF), "tag");
    }

    @Test
    public void testFilterQuery() throws IOException {
        assertTokenStreamContents(createFilter(TEST_HTTP_URL, URLPart.QUERY), "foo=bar&baz=bat");
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

    private URLTokenFilter createFilter(final String url, final URLPart part) {
        return createFilter(url, part, false);
    }

    private URLTokenFilter createFilter(final String url, final URLPart part, final boolean urlDecode) {
        int length = 0;
        if (url != null) {
            length = url.length();
        }
        return new URLTokenFilter(new SingleTokenTokenStream(new Token(url, 0, length)), part, urlDecode);
    }

    private static void assertTokenStreamContents(TokenStream in, String output) throws IOException {
        assertTokenStreamContents(in, new String[]{output});
    }
}