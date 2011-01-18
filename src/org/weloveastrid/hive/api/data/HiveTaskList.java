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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

/**
 *
 * @author Will Ross Jun 22, 2007
 */
@SuppressWarnings("nls")
public class HiveTaskList extends HiveData {

  private final String id;

  private final List<HiveTaskSeries> series;

  public HiveTaskList(String id) {
    this.id = id;
    this.series = new ArrayList<HiveTaskSeries>();
  }

  public HiveTaskList(Element elt) {
    id = elt.getAttribute("id");
    series = new ArrayList<HiveTaskSeries>();
    for (Element seriesElt : children(elt, "taskseries")) {
      series.add(new HiveTaskSeries(this, seriesElt));
    }

//    if (id == null || id.length() == 0) { throw new RuntimeException("No id found in task list."); }
  }

  public String getId() {
    return id;
  }

  public List<HiveTaskSeries> getSeries() {
    return Collections.unmodifiableList(series);
  }
}
