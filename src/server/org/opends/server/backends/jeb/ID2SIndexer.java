/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 */
package org.opends.server.backends.jeb;

import org.opends.server.types.Entry;
import org.opends.server.types.Modification;

import com.sleepycat.je.DatabaseEntry;

import java.util.*;

/**
 * Implementation of an Indexer for the subtree index.
 */
public class ID2SIndexer extends Indexer
{
  /**
   * The comparator for keys generated by this class.
   */
  private static final Comparator<byte[]> comparator =
       new AttributeIndex.KeyComparator();

  /**
   * Create a new indexer for a subtree index.
   */
  public ID2SIndexer()
  {
  }

  /**
   * Get a string representation of this object.  The returned value is
   * used to name an index created using this object.
   * @return A string representation of this object.
   */
  public String toString()
  {
    return "id2subtree";
  }

  /**
   * Get the comparator that must be used to compare index keys
   * generated by this class.
   *
   * @return A byte array comparator.
   */
  public Comparator<byte[]> getComparator()
  {
    return comparator;
  }

  /**
   * Generate the set of index keys for an entry.
   *
   * @param entry The entry.
   * @param addKeys The set into which the generated keys will be inserted.
   */
  public void indexEntry(Entry entry, Set<byte[]> addKeys)
  {
    // The superior entry IDs are in the entry attachment.
    ArrayList ids = (ArrayList)entry.getAttachment();

    // Skip the entry's own ID.
    Iterator iter = ids.iterator();
    iter.next();

    // Iterate through the superior IDs.
    while (iter.hasNext())
    {
      DatabaseEntry nodeIDData = ((EntryID)iter.next()).getDatabaseEntry();
      addKeys.add(nodeIDData.getData());
    }
  }

  /**
   * Generate the set of index keys to be added and the set of index keys
   * to be deleted for an entry that has been replaced.
   *
   * @param oldEntry The original entry contents.
   * @param newEntry The new entry contents.
   * @param modifiedKeys The map into which the modified keys will be inserted.
   */
  public void replaceEntry(Entry oldEntry, Entry newEntry,
                           Map<byte[], Boolean> modifiedKeys)
  {
    // Nothing to do.
  }



  /**
   * Generate the set of index keys to be added and the set of index keys
   * to be deleted for an entry that was modified.
   *
   * @param oldEntry The original entry contents.
   * @param newEntry The new entry contents.
   * @param mods The set of modifications that were applied to the entry.
   * @param modifiedKeys The map into which the modified keys will be inserted.
   */
  public void modifyEntry(Entry oldEntry, Entry newEntry,
                          List<Modification> mods,
                          Map<byte[], Boolean> modifiedKeys)
  {
    // Nothing to do.
  }
}
