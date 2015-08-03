package org.elasticsearch.plugin.analysis;

import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.URLTokenAnalysisBinderProcessor;
import org.elasticsearch.plugins.AbstractPlugin;

/**
 * Joe Linn
 * 1/17/2015
 */
public class AnalysisURLPlugin extends AbstractPlugin {
    /**
     * The name of the plugin.
     */
    @Override
    public String name() {
        return "analysis-url";
    }

    /**
     * The description of the plugin.
     */
    @Override
    public String description() {
        return "URL tokenizer and token filter.";
    }

    public void onModule(AnalysisModule module) {
        module.addProcessor(new URLTokenAnalysisBinderProcessor());
    }
}
