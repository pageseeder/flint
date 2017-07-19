package org.pageseeder.flint.catalog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.pageseeder.flint.indexing.FlintField;
import org.pageseeder.flint.indexing.FlintField.DocValuesType;
import org.pageseeder.flint.indexing.FlintField.NumericType;
import org.pageseeder.flint.indexing.FlintField.Resolution;
import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.XMLWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class Catalogs {

  /**
   * Private internal logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(Catalogs.class);

  /**
   * Internal cache.
   */
  private static final HashMap<String, Catalog> CACHE = new HashMap<>();

  /**
   * Root folder for catalog files.
   */
  private static File ROOT = null;

  /**
   * @param root the new root folder for catalog files.
   */
  public static void setRoot(File root) {
    ROOT = root;
  }

  /**
   * Store a new catalog.
   * 
   * @param catalog the new catalog
   */
  public static void newField(String catalog, FlintField builder) {
    if (catalog == null || builder == null) return;
    // find existing one
    Catalog cat = getCatalog(catalog);
    // create it?
    if (cat == null) {
      cat = new Catalog(catalog);
      putCatalog(cat);
    }
    cat.addFieldType(builder);
  }

  /**
   * Store a new catalog.
   * 
   * @param catalog the new catalog
   */
  public static void putCatalog(Catalog catalog) {
    if (catalog == null) return;
    CACHE.put(catalog.name(), catalog);
  }

  /**
   * Get a catalog from the internal cache or the persistent one.
   * 
   * @param name the catalog name.
   * 
   * @return the catalog if found, <code>null</code> otherwise.
   */
  public static Catalog getCatalog(String name) {
    if (name == null) return null;
    // cached?
    Catalog cat = CACHE.get(name);
    if (cat == null) {
      // ok load it from persistent cache
      cat = loadCatalog(name);
      if (cat != null) CACHE.put(name, cat);
    }
    return cat;
  }

  /**
   * Save the catalog specified in the persistent cache.
   * Nothing happens if catalog is not found.
   * 
   * @param catalog the catalog name
   */
  public static void save(String catalog) {
    Catalog thecatalog = CACHE.get(catalog);
    if (thecatalog != null) saveCatalog(thecatalog);
  }

  /**
   * Save all the catalog in the persistent cache.
   */
  public static void saveAll() {
    for (Catalog catalog : CACHE.values()) {
      saveCatalog(catalog);
    }
  }

  // ---------------------- private helpers --------------------------------

  /**
   * Save a catalog in the persistent cache.
   * 
   * @param catalog the catalog to save.
   */
  private static void saveCatalog(Catalog catalog) {
    // is there a place to store them?
    if (ROOT == null) {
      LOGGER.error("Trying to save catalog file when no root folder has been specified");
      return;
    }
    // find catalog file
    File file = new File(ROOT, catalog.name()+"-catalog.xml");
    // make sure parent folders exist
    file.getParentFile().mkdirs();
    // make sure it exists or can be created
    try {
      if (!file.exists() && !file.createNewFile()) {
        LOGGER.warn("Failed to create new catalog file for {}", catalog.name());
        return;
      }
    } catch (IOException ex) {
      LOGGER.warn("Failed to create new catalog file for {}", catalog.name(), ex);
    }
    // write file
    try (FileWriter out = new FileWriter(file)) {
      // use xml writer
      XMLWriter xml = new XMLWriterImpl(out, true);
      catalog.toXML(xml);
      xml.close();
    } catch (IOException ex) {
      LOGGER.warn("Failed to save catalog file for {}", catalog.name(), ex); 
    }
  }

  /**
   * Load a catalog from persistent cache.
   * 
   * @param name the catalog name
   * 
   * @return the catalog if found, <code>null</code> otherwise.
   */
  private static Catalog loadCatalog(String name) {
    // is there a place to store them?
    if (ROOT == null) return null;
    // find catalog file
    File file = new File(ROOT, name+"-catalog.xml");
    if (!file.exists()) {
      LOGGER.warn("Looking for inexistent catalog file for {}", name);
      return null;
    }
    // parse it
    CatalogHandler handler = new CatalogHandler(name);
    try (FileInputStream in = new FileInputStream(file)) {
      XMLReader reader = XMLReaderFactory.createXMLReader();
      reader.setContentHandler(handler);
      reader.parse(new InputSource(in));
    } catch (IOException ex) {
      LOGGER.error("Failed to load catalog file for {}", name, ex);
      return null;
    } catch (SAXException ex) {
      LOGGER.error("Failed to parse catalog file for {}", name, ex);
      return null;
    }
    // handler built the catalog
    return handler.catalog;
  }

  /**
   * SAX handler used to build a catalog.
   */
  public static class CatalogHandler extends DefaultHandler {

    /** The catalog object. */
    private final Catalog catalog;

    /** @param name the name of the catalog. */
    public CatalogHandler(String name) {
      this.catalog = new Catalog(name);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if ("field".equals(localName)) {
        String name = attributes.getValue("name");
        if (name != null) {
          boolean stored    = "true".equals(attributes.getValue("stored"));
          boolean tokenized = "true".equals(attributes.getValue("tokenized"));
          String b = attributes.getValue("boost");
          float boost = b == null ? 1.0F : Float.parseFloat(b);
          NumericType num     = FlintField.toNumeric(attributes.getValue("numeric-type"));
          DocValuesType dv    = FlintField.toDocValues(attributes.getValue("doc-values"), num != null);
          SimpleDateFormat df = FlintField.toDateFormat(attributes.getValue("date-format"));
          Resolution res      = FlintField.toResolution(attributes.getValue("date-resolution"));
          this.catalog.addFieldType(stored, name, tokenized, dv, num, df, res, boost);
        }
      }
    }
  }
}
