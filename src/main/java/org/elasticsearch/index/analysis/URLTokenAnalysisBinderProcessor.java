package org.elasticsearch.index.analysis;

import org.elasticsearch.index.analysis.url.URLTokenFilter;
import org.elasticsearch.index.analysis.url.URLTokenizer;

/**
 * Joe Linn
 * 1/17/2015
 */
public class URLTokenAnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {
    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {
        tokenFiltersBindings.processTokenFilter(URLTokenFilter.NAME, URLTokenFilterFactory.class);
    }


    @Override
    public void processTokenizers(TokenizersBindings tokenizersBindings) {
        tokenizersBindings.processTokenizer(URLTokenizer.NAME, URLTokenizerFactory.class);
    }
}
