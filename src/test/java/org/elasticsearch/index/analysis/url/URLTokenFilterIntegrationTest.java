package org.elasticsearch.index.analysis.url;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.test.ElasticsearchSingleNodeTest;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.analysis.url.URLTokenFilterTest.TEST_HTTPS_URL;
import static org.elasticsearch.index.analysis.url.URLTokenFilterTest.TEST_HTTP_URL;
import static org.hamcrest.Matchers.hasSize;

/**
 * Joe Linn
 * 1/17/2015
 */
public class URLTokenFilterIntegrationTest extends ElasticsearchSingleNodeTest {
    private static final String INDEX = "url_token_filter";

    /**
     * For subclasses to override. Overrides must call {@code super.setUp()}.
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        String settings = Streams.copyToStringFromClasspath("/test-settings.json");
        String mapping = Streams.copyToStringFromClasspath("/test-mapping.json");
        client().admin().indices().prepareCreate(INDEX).setSettings(settings).addMapping("test", mapping).get();
        refresh();
        Thread.sleep(75);   // Ensure that the shard is available before we start making analyze requests.
    }

    @Test
    public void testAnalyze() throws InterruptedException {

        assertURLAnalyzesTo(TEST_HTTP_URL, "url_protocol", "http");
        assertURLAnalyzesTo(TEST_HTTPS_URL, "url_protocol", "https");

        assertURLAnalyzesTo(TEST_HTTP_URL, "url_host", "www.foo.bar.com");

        assertURLAnalyzesTo(TEST_HTTP_URL, "url_port", "9200");
    }

    @Test(expected = ElasticsearchException.class)
    public void testInvalidURL() {
        analyzeURL("foobar", "url_protocol");
    }

    @Test
    public void testEmptyString() {
        List<AnalyzeResponse.AnalyzeToken> tokens = analyzeURL("", "url_protocol");
        assertThat("no tokens", tokens, hasSize(0));
    }

    @Test
    public void testUrlDecode() {
        assertURLAnalyzesTo("https://foo.bar.com?email=foo%40bar.com", "url_query", "email=foo@bar.com");
    }

    @Test
    public void testMalformed() {
        assertURLAnalyzesTo("foo.bar.com:444/baz", "url_port_malformed", "444");

        Map<String, Object> doc = new HashMap<>();
        doc.put("url_malformed", "foo.bar/baz/bat");
        client().prepareIndex(INDEX, "test").setSource(doc).get();
        refresh();

        SearchHits hits = client()
                .prepareSearch(INDEX)
                .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), FilterBuilders.missingFilter("http_malformed.port")))
                .get()
                .getHits();
        assertEquals("found a doc missing http_malformed.port", 1, hits.getTotalHits());
    }

    @Test
    public void testIndex() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("url", "http://foo.bar/baz/bat");
        client().prepareIndex(INDEX, "test").setSource(doc).get();
        doc.put("url", "https://foo.bar.com");
        client().prepareIndex(INDEX, "test").setSource(doc).get();
        refresh();

        SearchHits hits = client().prepareSearch(INDEX).setQuery(QueryBuilders.matchAllQuery()).get().getHits();
        assertEquals("both docs indexed", 2, hits.getTotalHits());
    }

    private void refresh() {
        client().admin().indices().prepareRefresh().get();
    }

    private void assertURLAnalyzesTo(String url, String analyzer, String expected) {
        List<AnalyzeResponse.AnalyzeToken> tokens = analyzeURL(url, analyzer);
        assertThat("a URL part was parsed", tokens, hasSize(1));
        assertEquals("term value", expected, tokens.get(0).getTerm());
    }

    private List<AnalyzeResponse.AnalyzeToken> analyzeURL(String url, String analyzer) {
        return client().admin().indices().prepareAnalyze(INDEX, url).setAnalyzer(analyzer).get().getTokens();
    }
}
