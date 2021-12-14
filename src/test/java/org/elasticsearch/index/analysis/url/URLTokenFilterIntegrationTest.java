package org.elasticsearch.index.analysis.url;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.analysis.url.URLTokenFilterTest.TEST_HTTPS_URL;
import static org.elasticsearch.index.analysis.url.URLTokenFilterTest.TEST_HTTP_URL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Joe Linn
 * 1/17/2015
 */
@Ignore
public class URLTokenFilterIntegrationTest extends URLAnalysisTestCase {

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
        List<AnalyzeAction.AnalyzeToken> tokens = analyzeURL("", "url_protocol");
        assertThat("no tokens", tokens, hasSize(0));
    }

    @Test
    public void testUrlDecode() {
        assertURLAnalyzesTo("https://foo.bar.com?email=foo%40bar.com", "url_query", "email=foo@bar.com");
        assertURLAnalyzesTo("https://ssl.google-analytics.com/r/__utm.gif?utmwv=5.6.4&utms=1&utmn=1031590447&utmhn=www.linkedin.com&utmcs=-&utmsr=1024x768&utmvp=1256x2417&utmsc=24-bit&utmul=en-us&utmje=1&utmfl=-&utmdt=Wells%20Fargo%20Capital%20Finance%20%7C%20LinkedIn&utmhid=735221740&utmr=http%3A%2F%2Fwww.google.com%2Fsearch%3Fq%3Dsite%253Alinkedin.com%2Bwells%2Bfargo%26rls%3Dcom.microsoft%3Aen-us%26ie%3DUTF-8%26oe%3DUTF-8%26startIndex%3D%26startPage%3D1&utmp=biz-overview-public&utmht=1428449620694&utmac=UA-3242811-1&utmcc=__utma%3D23068709.1484257758.1428449621.1428449621.1428449621.1%3B%2B__utmz%3D23068709.1428449621.1.1.utmcsr%3Dgoogle%7Cutmccn%3D(organic)%7Cutmcmd%3Dorganic%7Cutmctr%3Dsite%253Alinkedin.com%2520wells%2520fargo%3B&utmjid=1336170366&utmredir=1&utmu=qBCAAAAAAAAAAAAAAAAAAAAE~", "url_port", "443");
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
                .setQuery(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("http_malformed.port")))
                .get()
                .getHits();
        assertEquals("found a doc missing http_malformed.port", 1, hits.getTotalHits());
    }


    @Test
    public void testPassthrough() {
        List<AnalyzeAction.AnalyzeToken> tokens = analyzeURL("http://foo.com:9200/foo.bar baz bat.blah", "url_host_passthrough");
        assertThat(tokens, hasSize(4));
        assertThat(tokens.get(0).getTerm(), equalTo("foo.com"));
        assertThat(tokens.get(1).getTerm(), equalTo("com"));
        assertThat(tokens.get(2).getTerm(), equalTo("baz"));
        assertThat(tokens.get(3).getTerm(), equalTo("bat.blah"));
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

    private void assertURLAnalyzesTo(String url, String analyzer, String expected) {
        List<AnalyzeAction.AnalyzeToken> tokens = analyzeURL(url, analyzer);
        assertThat("a URL part was parsed", tokens, hasSize(1));
        assertEquals("term value", expected, tokens.get(0).getTerm());
    }
}
