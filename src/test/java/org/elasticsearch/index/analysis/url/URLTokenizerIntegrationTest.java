package org.elasticsearch.index.analysis.url;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * Joe Linn
 * 8/1/2015
 */
public class URLTokenizerIntegrationTest extends URLAnalysisTestCase {
    @Test
    public void testAnalyze() {
        assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_protocol", "http");
        assertTokensContain(URLTokenizerTest.TEST_HTTPS_URL, "tokenizer_url_protocol", "https");

        assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_host", "www.foo.bar.com", "foo.bar.com", "bar.com", "com");
        List<AnalyzeResponse.AnalyzeToken> hostTokens = assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_host_single", "www.foo.bar.com");
        assertThat(hostTokens, hasSize(1));

        assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_all", "www.foo.bar.com:9200", "http://www.foo.bar.com");

        assertTokensContain("foo.bar.com/baz.html/query?a=1", "tokenizer_url_all_malformed", "foo.bar.com", "/baz.html/query");
    }


    @Test
    public void testAnalyzeWhole() throws Exception {
        List<AnalyzeResponse.AnalyzeToken> tokens = analyzeURL("http://foo.bar.com", "tokenizer_url_all_malformed");
        assertThat(tokens, notNullValue());
        assertThat(tokens, hasSize(7));
    }


    private List<AnalyzeResponse.AnalyzeToken> assertTokensContain(String url, String analyzer, String... expected) {
        List<AnalyzeResponse.AnalyzeToken> tokens = analyzeURL(url, analyzer);
        for (String e : expected) {
            assertThat(tokens, hasItem(Matchers.<AnalyzeResponse.AnalyzeToken>hasProperty("term", equalTo(e))));
        }
        return tokens;
    }
}
