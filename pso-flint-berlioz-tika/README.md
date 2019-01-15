[ ![Download](https://api.bintray.com/packages/pageseeder/maven/flint/images/download.svg) ](https://bintray.com/pageseeder/maven/flint/_latestVersion)

# berlioz-flint-tika

TIKA dependent library to be used with flint-berlioz to index binary files.

Currently supported file extensions are:
  - pdf
  - docx
  - doc
  - pptx
  - ppt
  - rtf
  - jpeg
  - jpg
  - png
  - bmp
  - gif
  - txt
  - html 

The file content is parsed by TIKA auto parser and the output is used as
the source to be transformed by the XSLT producing iXML content understood by Flint.
The content is wrapped in the element `<content source="tika">`, for example:

```<content source="tika">
  <html>
    <head>
      <meta name="date" content="2003-06-01T19:39:59Z"/>
      <meta name="pdf:PDFVersion" content="1.3"/>
      <meta name="Content-Type" content="application/pdf"/>
      ...
    </head>
    <body>
      <div class="page">[PDF content]</div>
    </body>
  </html>
</content>```
