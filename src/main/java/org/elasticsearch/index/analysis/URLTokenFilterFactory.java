package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.url.URLTokenFilter;
import org.elasticsearch.index.settings.IndexSettingsService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Joe Linn
 * 1/17/2015
 */
@AnalysisSettingsRequired
public class URLTokenFilterFactory extends AbstractTokenFilterFactory {
    private final List<URLPart> parts;
    private final boolean urlDecode;
    private boolean tokenizeHost;
    private boolean tokenizePath;
    private boolean tokenizeQuery;
    private final boolean allowMalformed;
    private final boolean tokenizeMalformed;
    private final boolean passthrough;

    @Inject
    public URLTokenFilterFactory(Index index, IndexSettingsService indexSettings, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings.indexSettings(), name, settings);

        this.parts = Arrays.stream(settings.getAsArray("part", new String[]{"whole"}))
                .map(URLPart::fromString)
                .collect(Collectors.toList());

        this.urlDecode = settings.getAsBoolean("url_decode", false);
        this.tokenizeHost = settings.getAsBoolean("tokenize_host", true);
        this.tokenizePath = settings.getAsBoolean("tokenize_path", true);
        this.tokenizeQuery = settings.getAsBoolean("tokenize_query", true);
        this.allowMalformed = settings.getAsBoolean("allow_malformed", false);
        this.tokenizeMalformed = settings.getAsBoolean("tokenize_malformed", false);
        this.passthrough = settings.getAsBoolean("passthrough", false);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new URLTokenFilter(tokenStream, null, urlDecode, allowMalformed, passthrough)
                .setParts(parts)
                .setTokenizeMalformed(tokenizeMalformed)
                .setTokenizeHost(tokenizeHost)
                .setTokenizePath(tokenizePath)
                .setTokenizeQuery(tokenizeQuery);
    }
}
