package org.elasticsearch.index.analysis.url;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.plugin.analysis.AnalysisURLPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.StreamsUtils;
import org.junit.Before;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Joe Linn
 * 8/1/2015
 */
public abstract class URLAnalysisTestCase extends ESIntegTestCase {
    protected static final String INDEX = "url_token_filter";
    protected static final String TYPE = "test";


    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(AnalysisURLPlugin.class);
    }

    /**
     * For subclasses to override. Overrides must call {@code super.setUp()}.
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        String settings = StreamsUtils.copyToStringFromClasspath("/test-settings.json");
        String mapping = StreamsUtils.copyToStringFromClasspath("/test-mapping.json");
        client().admin().indices().prepareCreate(INDEX).setSettings(settings, XContentType.JSON).addMapping(TYPE, mapping, XContentType.JSON).get();
        refresh();
        Thread.sleep(75);   // Ensure that the shard is available before we start making analyze requests.
    }

    protected List<AnalyzeAction.AnalyzeToken> analyzeURL(String url, String analyzer) {
        return client().admin().indices().prepareAnalyze(INDEX, url).setAnalyzer(analyzer).get().getTokens();
    }
}
