<?xml version="1.0"?>
<!--
  The default stylesheet to generate an Indexable document from source XML.

  @author Christophe Lauret
-->
<xsl:stylesheet version="2.0" xmlns:idx="http://weborganic.com/Berlioz/Index"
                              xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:ps="http://www.pageseeder.com/editing/2.0"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema"
                              exclude-result-prefixes="#all">

<!-- Standard output for Flint Documents 2.0 -->
<xsl:output method="xml" indent="no" encoding="utf-8"
            doctype-public="-//Weborganic//DTD::Flint Index Documents 5.0//EN"
            doctype-system="http://weborganic.org/schema/flint/index-documents-5.0.dtd"/>

<!-- Send by the indexer -->
<xsl:param name="_src" />
<xsl:param name="_path" />
<xsl:param name="_filename" />

<!-- Matches the root -->
<xsl:template match="/">
  <documents version="5.0">
    <xsl:apply-templates/>
  </documents>
</xsl:template>

<!-- ========================================================================================= -->
<!-- PSML docs                                                                                  -->
<!-- ========================================================================================= -->

<xsl:template match="document">
  <document>
    <!-- Common fields -->
    <field name="_src"  tokenize="false"><xsl:value-of select="$_src"/></field>
    <field name="_path" tokenize="false"><xsl:value-of select="$_path"/></field>
    <field name="type"  tokenize="false"><xsl:value-of select="@type"/></field>
    <field name="title" tokenize="false"><xsl:value-of select="if (.//heading) then (.//heading)[1] else $_filename"/></field>

    <!-- Single value properties -->
    <xsl:for-each select="descendant::property[@value]">
      <field name="{@name}" tokenize="false"><xsl:value-of select="@value"/></field>
    </xsl:for-each>

    <!-- Multiple value properties -->
    <xsl:for-each select="descendant::property[value]">
      <field name="{@name}" tokenize="false"><xsl:value-of select="value" separator=","/></field>
    </xsl:for-each>
    
    <!-- full text -->
    <field name="fulltext" tokenize="true"><xsl:value-of select="string-join(.//* | .//property/@value, ',')"/></field>

    <!-- PageSeeder documents - ->
    <xsl:apply-templates select="descendant::fragment/*" mode="index"/>
    -->
  </document>
</xsl:template>


</xsl:stylesheet>
