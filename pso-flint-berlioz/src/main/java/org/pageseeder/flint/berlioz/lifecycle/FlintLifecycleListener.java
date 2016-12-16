package org.pageseeder.flint.berlioz.lifecycle;

import java.io.File;

import org.pageseeder.berlioz.LifecycleListener;
import org.pageseeder.flint.berlioz.model.FlintConfig;
import org.pageseeder.flint.berlioz.model.SolrIndexMaster;
import org.pageseeder.flint.solr.SolrFlintException;

public class FlintLifecycleListener implements LifecycleListener {

  @Override
  public boolean start() {
    System.out.println("[BERLIOZ_INIT] Lifecycle: Loading Flint Indexes");
    FlintConfig config = FlintConfig.get();
    int nb = 0;
    if (config.useSolr()) {
      try {
        for (SolrIndexMaster index : config.listSolrIndexes()) {
          System.out.println("[BERLIOZ_INIT] Lifecycle: index" + index.getName() + " successfuly loaded.");
          nb ++;
        }
      } catch (SolrFlintException ex) {
        if (ex.cannotConnect()) {
          System.out.println("[BERLIOZ_INIT] Lifecycle: Failed to connect to Solr server, please check configuration!");
        } else {
          System.out.println("[BERLIOZ_INIT] Lifecycle: Failed to list Solr indexes: "+ex.getMessage()+".");
        }
      }
    } else {
      File root = config.getRootDirectory();
      for (File folder : root.listFiles()) {
        if (folder.isDirectory()) {
          // autosuggest index?
          if (folder.getName().endsWith("_autosuggest") &&
              new File(root, folder.getName().split("_")[0]).exists()) {
            continue;
          }
          if (config.getMaster(folder.getName(), true) == null) {
            System.out.println("[BERLIOZ_INIT] Lifecycle: Failed to load index "+folder.getName());
          } else {
            nb++;
          }
        }
      }
    }
    System.out.println("[BERLIOZ_INIT] Lifecycle: Successfully loaded "+nb+" index"+(nb == 1 ? "" : "es"));
    return true;
  }

  @Override
  public boolean stop() {
    System.out.println("[BERLIOZ_STOP] Lifecycle: Closing Flint Indexes");
    // stop it all
    FlintConfig.get().stop();
    return true;
  }

}
