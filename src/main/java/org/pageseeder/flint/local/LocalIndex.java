/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.flint.local;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.pageseeder.flint.Index;
import org.pageseeder.flint.content.Content;
import org.pageseeder.flint.content.DeleteRule;
import org.pageseeder.flint.indexing.FlintField;

/**
 * A basic implementation of a local index.
 *
 * @author Christophe Lauret
 * @version 27 February 2013
 */
public abstract class LocalIndex extends Index {

  /**
   * Create a new local index.
   */
  public LocalIndex(String name, String catalog) {
    super(name, catalog);
  }

  @Override
  public Map<String, String> getParameters(Content content) {
    if (content.getContentType() == LocalFileContentType.SINGLETON) {
      return getParameters(new File(content.getContentID()));
    }
    return null;
  }

  @Override
  public Collection<FlintField> getFields(Content content) {
    if (content.getContentType() == LocalFileContentType.SINGLETON) {
      return getFields(new File(content.getContentID()));
    }
    return null;
  }

  @Override
  public String toString() {
    return getIndexID();
  }

  public void setTemplate(String extension, URI template) throws TransformerException {
    setTemplates(LocalFileContentType.SINGLETON, extension, template);
  }

  public abstract File getContentLocation();

  public Map<String, String> getParameters(File file) {
    return null;
  }
  public Collection<FlintField> getFields(File file) {
    return null;
  }
  public DeleteRule getDeleteRule(File file) {
    return null;
  }

}
