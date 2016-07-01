<?xml version="1.0"?>
<!--
  The default stylesheet to generate an Indexable document from source XML.

  @version BerliozBase-0.8.1
-->
<xsl:stylesheet version="2.0" xmlns:idx="http://weborganic.com/Berlioz/Index"
                              xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema"
                              exclude-result-prefixes="#all">

<!-- Standard output for Flint Documents 5.0 -->
<xsl:output method="xml" indent="no" encoding="utf-8"
            doctype-public="-//Weborganic//DTD::Flint Index Documents 5.0//EN"
            doctype-system="http://weborganic.org/schema/flint/index-documents-5.0.dtd"/>

<xsl:template match="/">
  <xsl:apply-templates />
</xsl:template>

<xsl:template match="*">
  <xsl:copy>
    <xsl:copy-of select="@*" />
    <xsl:apply-templates />
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>