package org.elasticsearch.index.analysis;

import com.google.common.base.Strings;
import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.url.URLTokenizer;
import org.elasticsearch.index.settings.IndexSettings;

/**
 * Joe Linn
 * 8/1/2015
 */
@AnalysisSettingsRequired
public class URLTokenizerFactory extends AbstractTokenizerFactory {
    private URLPart part;
    private boolean urlDecode;
    private boolean tokenizeHost;
    private boolean tokenizePath;
    private boolean tokenizeQuery;
    private boolean allowMalformed;


    @Inject
    public URLTokenizerFactory(Index index, @IndexSettings Settings indexSettings, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings, name, settings);

        String partString = settings.get("part");
        if (!Strings.isNullOrEmpty(partString)) {
            this.part = URLPart.fromString(partString);
        }
        this.urlDecode = settings.getAsBoolean("url_decode", false);
        this.tokenizeHost = settings.getAsBoolean("tokenize_host", true);
        this.tokenizePath = settings.getAsBoolean("tokenize_path", true);
        this.tokenizeQuery = settings.getAsBoolean("tokenize_query", true);
        this.allowMalformed = settings.getAsBoolean("allow_malformed", false);
    }


    @Override
    public Tokenizer create() {
        URLTokenizer tokenizer = new URLTokenizer();
        tokenizer.setPart(part);
        tokenizer.setUrlDecode(urlDecode);
        tokenizer.setTokenizeHost(tokenizeHost);
        tokenizer.setTokenizePath(tokenizePath);
        tokenizer.setTokenizeQuery(tokenizeQuery);
        tokenizer.setAllowMalformed(allowMalformed);
        return tokenizer;
    }
}
