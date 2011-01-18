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
import java.util.List;

import org.w3c.dom.Element;

/**
 * Represents the notes of a task.
 * 
 * @author Edouard Mercier
 * @since 2008.04.22
 */
@SuppressWarnings("nls")
public class HiveTaskNotes
    extends HiveData
{

  private List<HiveTaskNote> notes;

  public HiveTaskNotes(Element element)
  {
    notes = new ArrayList<HiveTaskNote>();
    for (Element child : children(element, "note"))
    {
      notes.add(new HiveTaskNote(child));
    }
  }

  public List<HiveTaskNote> getNotes()
  {
    return notes;
  }

}
