package org.elasticsearch.index.analysis.url;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.test.ElasticsearchSingleNodeTest;
import org.junit.Before;

import java.util.List;

/**
 * Joe Linn
 * 8/1/2015
 */
public abstract class URLAnalysisTestCase extends ElasticsearchSingleNodeTest {
    protected static final String INDEX = "url_token_filter";

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

    protected void refresh() {
        client().admin().indices().prepareRefresh().get();
    }

    protected List<AnalyzeResponse.AnalyzeToken> analyzeURL(String url, String analyzer) {
        return client().admin().indices().prepareAnalyze(INDEX, url).setAnalyzer(analyzer).get().getTokens();
    }
}
