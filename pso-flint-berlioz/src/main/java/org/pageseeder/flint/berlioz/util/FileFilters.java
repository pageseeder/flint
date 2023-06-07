/*
 * Decompiled with CFR 0_110.
 */
package org.pageseeder.flint.berlioz.util;

import java.io.File;
import java.io.FileFilter;

public final class FileFilters {
    private static final FileFilter FOLDERS = File::isDirectory;
    private static final FileFilter XML = f -> f.isFile() && f.getName().toLowerCase().endsWith(".xml");
    private static final FileFilter PSML = f -> f.isFile() && f.getName().toLowerCase().endsWith(".psml");
    private static final FileFilter XSLT = f -> f.isFile() && f.getName().toLowerCase().endsWith(".xsl");

    private FileFilters() {
    }

    public static FileFilter getFolders() {
        return FOLDERS;
    }

    public static FileFilter getXMLFiles() {
        return XML;
    }

    public static FileFilter getPSMLFiles() {
        return PSML;
    }

    public static FileFilter getXSLTFiles() {
        return XSLT;
    }

}

