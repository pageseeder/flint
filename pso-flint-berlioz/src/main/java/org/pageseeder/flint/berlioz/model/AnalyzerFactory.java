/*
 * Decompiled with CFR 0_110.
 *
 * Could not load the following classes:
 *  org.apache.lucene.analysis.Analyzer
 */
package org.pageseeder.flint.berlioz.model;

import org.apache.lucene.analysis.Analyzer;

public interface AnalyzerFactory {
    /**
     * @deprecated
     */
    Analyzer getAnalyzer();
    Analyzer getAnalyzer(IndexDefinition definition);
}

