package org.elasticsearch.index.analysis.url;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

/**
 * Joe Linn
 * 8/1/2015
 */
@Ignore
public class URLTokenizerIntegrationTest extends URLAnalysisTestCase {
    @Test
    public void testAnalyze() {
        assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_protocol", "http");
        assertTokensContain(URLTokenizerTest.TEST_HTTPS_URL, "tokenizer_url_protocol", "https");

        assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_host", "www.foo.bar.com", "foo.bar.com", "bar.com", "com");
        List<AnalyzeAction.AnalyzeToken> hostTokens = assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_host_single", "www.foo.bar.com");
        assertThat(hostTokens, hasSize(1));

        assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_all", "www.foo.bar.com:9200", "http://www.foo.bar.com");

        assertTokensContain(URLTokenizerTest.TEST_HTTP_URL, "tokenizer_url_protocol_and_host", "http", "www.foo.bar.com", "foo.bar.com", "bar.com", "com");

        assertTokensContain("foo.bar.com/baz.html/query?a=1", "tokenizer_url_all_malformed", "foo.bar.com", "/baz.html/query");
    }


    @Test
    public void testAnalyzeWhole() throws Exception {
        List<AnalyzeAction.AnalyzeToken> tokens = analyzeURL("http://foo.bar.com", "tokenizer_url_all_malformed");
        assertThat(tokens, notNullValue());
        assertThat(tokens, hasSize(7));
    }


    @Test
    public void testHighlight() throws Exception {
        final String field = "url_highlight_test";
        Map<String, String> docContent = new HashMap<>();
        final String url = "http://www.foo.bar.com:8080/baz/bat?bob=blah";
        docContent.put(field, url);
        client().prepareIndex(INDEX, TYPE).setSource(docContent).get();
        refresh(INDEX);

        SearchResponse response = client().prepareSearch(INDEX).setQuery(QueryBuilders.matchQuery(field, "www.foo.bar.com:8080"))
                .highlighter(new HighlightBuilder().preTags("<b>").postTags("</b>").field("*").forceSource(true))
                .get();

        SearchHit[] hits = response.getHits().getHits();
        assertThat(hits.length, equalTo(1));

        SearchHit hit = hits[0];
        Map<String, Object> source = hit.getSourceAsMap();
        assertThat(source.size(), equalTo(1));
        assertThat(source, hasKey(field));
        assertThat("URL was stored correctly", source.get(field), equalTo(url));
        assertThat(hit.getHighlightFields(), hasKey(field));
        HighlightField highlightField = hit.getHighlightFields().get(field);
        Text[] fragments = highlightField.getFragments();
        assertThat(fragments.length, equalTo(1));
        Text fragment = fragments[0];
        assertThat("URL was highlighted correctly", fragment.string(), equalTo("http://<b>www.foo.bar.com</b>:<b>8080</b>/baz/bat?bob=blah"));
    }


    @Test
    public void testBulkIndexing() throws Exception {
        final String field = "bulk_indexing_test";
        Map<String, String> content;
        final int numDocs = 100;
        BulkRequestBuilder bulkBuilder = client().prepareBulk();
        for (int i = 0; i < numDocs; i++) {
            content = new HashMap<>();
            content.put(field, "http://domain" + i + ".com/foo" + i + "/bar.html");
            bulkBuilder.add(client().prepareIndex(INDEX, TYPE).setSource(content));
        }
        BulkResponse bulkResponse = bulkBuilder.get();
        assertThat(bulkResponse.buildFailureMessage(), bulkResponse.hasFailures(), equalTo(false));
    }


    private List<AnalyzeAction.AnalyzeToken> assertTokensContain(String url, String analyzer, String... expected) {
        List<AnalyzeAction.AnalyzeToken> tokens = analyzeURL(url, analyzer);
        for (String e : expected) {
            assertThat(tokens, hasItem(Matchers.<AnalyzeAction.AnalyzeToken>hasProperty("term", equalTo(e))));
        }
        return tokens;
    }
}
