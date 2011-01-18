/*
 * Copyright 2007, MetaDimensional Technologies Inc.
 *
 *
 * This file is part of the RememberTheMilk Java API.
 *
 * The RememberTheMilk Java API is free software; you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * The RememberTheMilk Java API is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.weloveastrid.hive.api.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

@SuppressWarnings("nls")
public class HiveLists extends HiveData {

  private final Map<String, HiveList> lists;

  public HiveLists() {
    this.lists = new HashMap<String, HiveList>();
  }

  public HiveLists(Element elt) {
    this.lists = new HashMap<String, HiveList>();
    for (Element listElt : children(elt, "list")) {
      HiveList list = new HiveList(listElt);
      lists.put(list.getId(), list);
    }
  }

  public HiveList getList(String id) {
    return lists.get(id);
  }

  public Map<String, HiveList> getLists() {
    return Collections.unmodifiableMap(lists);
  }
}
