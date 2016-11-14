package org.elasticsearch.index.analysis;

import java.util.Comparator;

/**
 * @author Joe Linn
 *         11/13/2016
 */
public class URLPartComparator implements Comparator<URLPart> {
    @Override
    public int compare(URLPart o1, URLPart o2) {
        return o1.getOrder() - o2.getOrder();
    }
}
