# Flint

Flint library is using Lucene.

With Flint, you can easily read and write data into a Lucene index using a simple XML format (iXML).

The iXML format is a simple XML representation of a Lucene record, for example:

```xml
<document>
  <field name="id" store="yes" index="no">123</field>
  <field name="title" store="yes" index="un-tokenized">My Record</field>
  <field name="summary" store="compress" index="tokenized">This is important data I want to be able to search</field>
<document>
```

Flint can load iXML into a Lucene index, but it does not convert original format (XML, PDF, DOCX, HTML, text, etc…) into a Lucene index.

Flint also simplifies the way queries can be aggregated.
