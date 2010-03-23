package org.weborganic.flint.legacy;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.lucene.queryParser.ParseException;
import org.weborganic.flint.query.PredicateSearchQuery;
import org.weborganic.flint.query.SearchPaging;
import org.weborganic.flint.query.SearchResults;
import org.weborganic.flint.util.CommandLine;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * A utility class to perform a Lucene index search using the arguments received from the command-line.
 * 
 * <p>This class is used to used a Lucene predicate directly.
 * 
 * @author Jin Zhou (Allette Systems)
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 19 November 2009
 */
public final class DirectSearchMain {

  /**
   * Prevent creation of instances.
   */
  private DirectSearchMain() {
  }

  /**
   * Main method to invoke from the command line.
   * 
   * <p>Possible arguments are:
   * <pre>
   *     -index        [index files to use]    (compulsory)
   *     -predicate    [lucene predicate]      (compulsory)
   *     -paging       [result hits per page]   (optional,default '20')
   * </pre>
   * 
   * @param args The command line arguments.
   */
  public static void main(String[] args) {
    // read arguments from the command-line
    String index = CommandLine.getParameter("-index", args); // index to use
    String predicate = CommandLine.getParameter("-predicate", args); // predicate
    String paging = CommandLine.getParameter("-paging", args); // hits per page
    // perform the search
    try {
      search(index, predicate, paging);
    } catch (ParseException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

// methods ---------------------------------------------------------------------------------------

  /**
   * Displays the error message plus usage on <code>System.err</code>.
   * 
   * @param error The error thrown to display on the error output.
   */
  public static void usage(String error) {
    System.err.println("error: " + error);
    usage();
    System.exit(1);
  }

  /**
   * Displays the usage on <code>System.err</code>.
   */
  public static void usage() {
    System.err.println("usage: Indexer -index <index> <options>");
    System.err.println("  <index>     Specify the index folder to use");
    System.err.println("possible options include:");
    System.err.println("  -predicate <lucene predicate>   The predicate to use");
    System.err.println("  -paging <hits per page>  must be integer value (default '20')");
  }

  /**
   * Searches the specified index using the given predicate and paging.
   * 
   * @param index     The directory where the index to search is located (required).
   * @param predicate The Lucene search predicate to pass directly to Lucene. 
   * @param paging    An integer value
   * 
   * @throws ParseException If the Lucene predicate is invalid
   * @throws IOException    If an error occurs while reading the index or writing the results.
   */
  private static void search(String index, String predicate, String paging)
    throws ParseException, IOException {

    // do a check on these arguments
    if (index == null)
      usage("you must specify an index folder to read.");

    // locate the index folder on file system
    File indexFile = new File(index);

    // initialise the search and query
    IndexSearch isearch = new IndexSearch(indexFile);
    PredicateSearchQuery query = new PredicateSearchQuery(predicate);

    // initialise the paging
    SearchPaging searchpaging = new SearchPaging();
    if (paging != null) {
      try {
        searchpaging.setHitsPerPage(Integer.parseInt(paging));
      } catch (NumberFormatException ex) {
        usage("-paging arg must be an integer value");
      }
    }

    // Get search result
    SearchResults results = isearch.search(query, searchpaging);

    // Create an XML writer and serialise the search results as XML
    StringWriter writer = new StringWriter();
    XMLWriter xml = new XMLWriterImpl(writer, true);
    xml.xmlDecl();
    results.toXML(xml);
    xml.flush();

    // The index MUST BE closed in order to avoid the "too many open file" error.
    isearch.closeIndex();

    // Write the standard output.
    System.out.println(writer.toString());
  }

}
