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
package org.weloveastrid.hive.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Element;
import org.weloveastrid.hive.api.data.HiveAuth;
import org.weloveastrid.hive.api.data.HiveData;
import org.weloveastrid.hive.api.data.HiveFrob;
import org.weloveastrid.hive.api.data.HiveList;
import org.weloveastrid.hive.api.data.HiveLists;
import org.weloveastrid.hive.api.data.HiveLocation;
import org.weloveastrid.hive.api.data.HiveTask;
import org.weloveastrid.hive.api.data.HiveTask.Priority;
import org.weloveastrid.hive.api.data.HiveTaskList;
import org.weloveastrid.hive.api.data.HiveTaskNote;
import org.weloveastrid.hive.api.data.HiveTaskSeries;
import org.weloveastrid.hive.api.data.HiveTasks;
import org.weloveastrid.hive.api.data.HiveTimeline;


/**
 * A major part of the Hiveminder API implementation is here.
 *
 * @author Will Ross Jun 21, 2007
 * @author Edouard Mercier, since 2008.04.15
 * @author timsu January 2009
 */
@SuppressWarnings("nls")
public class ServiceImpl
{

  public final static String SERVER_HOST_NAME = "hiveminder.com"; //$NON-NLS-1$

  public final static int SERVER_PORT_NUMBER = 80;

  public final static String REST_SERVICE_URL_POSTFIX = "/services/rest/"; //$NON-NLS-1$

  private final ApplicationInfo applicationInfo;

  private final Invoker invoker;

  private final Prefs prefs;

  private String currentAuthToken;

  HiveFrob tempFrob;

  public ServiceImpl(ApplicationInfo applicationInfo)
      throws ServiceInternalException
  {
    invoker = new Invoker(SERVER_HOST_NAME, SERVER_PORT_NUMBER, REST_SERVICE_URL_POSTFIX, applicationInfo);
    this.applicationInfo = applicationInfo;
    prefs = new Prefs();
    if (applicationInfo.getAuthToken() != null)
    {
      currentAuthToken = applicationInfo.getAuthToken();
    }
    else
    {
      currentAuthToken = prefs.getAuthToken();
    }
  }

  public boolean isServiceAuthorized()
      throws ServiceException
  {
    if (currentAuthToken == null)
      return false;

    try
    {
      /* HiveAuth auth = */auth_checkToken(currentAuthToken);
      return true;
    }
    catch (ServiceException e)
    {
      if (e.getResponseCode() != 98)
      {
        throw e;
      }
      else
      {
        // Bad token.
        currentAuthToken = null;
        return false;
      }
    }
  }

  public String beginAuthorization(HiveAuth.Perms permissions)
      throws ServiceException
  {
    // Instructions from the "User authentication for desktop applications"
    // section at http://www.rememberthemilk.com/services/api/authentication.rtm
    tempFrob = auth_getFrob();
    return beginAuthorization(tempFrob, permissions);
  }

  public String beginAuthorization(HiveFrob frob, HiveAuth.Perms permissions)
      throws ServiceException
  {
    String authBaseUrl = "http://" + SERVER_HOST_NAME + "/services/auth/";
    Param[] params = new Param[] { new Param("api_key", applicationInfo.getApiKey()), new Param("perms", permissions.toString()),
        new Param("frob", frob.getValue()) };
    Param sig = new Param("api_sig", invoker.calcApiSig(params));
    StringBuilder authUrl = new StringBuilder(authBaseUrl);
    authUrl.append("?");
    for (Param param : params)
    {
      authUrl.append(param.getName()).append("=").append(param.getValue()).append("&");
    }
    authUrl.append(sig.getName()).append("=").append(sig.getValue());
    return authUrl.toString();
  }

  public String completeAuthorization()
      throws ServiceException
  {
    return completeAuthorization(tempFrob);
  }

  public String completeAuthorization(HiveFrob frob)
      throws ServiceException
  {
    currentAuthToken = auth_getToken(frob.getValue());
    prefs.setAuthToken(currentAuthToken);
    return currentAuthToken;
  }

  public HiveAuth auth_checkToken(String authToken)
      throws ServiceException
  {
    Element response = invoker.invoke(new Param("method", "rtm.auth.checkToken"), new Param("auth_token", authToken),
        new Param("api_key", applicationInfo.getApiKey()));
    return new HiveAuth(response);
  }

  public HiveFrob auth_getFrob()
      throws ServiceException
  {
    return new HiveFrob(invoker.invoke(new Param("method", "rtm.auth.getFrob"), new Param("api_key", applicationInfo.getApiKey())));
  }

  public String auth_getToken(String frob)
      throws ServiceException
  {
    Element response = invoker.invoke(new Param("method", "rtm.auth.getToken"), new Param("frob", frob), new Param("api_key", applicationInfo.getApiKey()));
    return new HiveAuth(response).getToken();
  }

  public void contacts_add()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void contacts_delete()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void contacts_getList()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void groups_add()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void groups_addContact()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void groups_delete()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void groups_getList()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void groups_removeContact()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public HiveList lists_add(String timelineId, String listName)
      throws ServiceException
  {
    Element response = invoker.invoke(new Param("method", "rtm.lists.add"), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()), new Param("name", listName), new Param("timeline", timelineId));
    return new HiveList(response);
  }

  public void lists_archive()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void lists_delete(String timelineId, String listId)
      throws ServiceException
  {
    invoker.invoke(new Param("method", "rtm.lists.delete"), new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()),
        new Param("timeline", timelineId), new Param("list_id", listId));
  }

  public HiveLists lists_getList()
      throws ServiceException
  {
    Element response = invoker.invoke(new Param("method", "rtm.lists.getList"), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
    return new HiveLists(response);
  }

  public HiveList lists_getList(String listName)
      throws ServiceException
  {
    HiveLists fullList = lists_getList();
    for (Entry<String, HiveList> entry : fullList.getLists().entrySet())
    {
      if (entry.getValue().getName().equals(listName))
      {
        return entry.getValue();
      }
    }
    return null;
  }

  public void lists_setDefaultList()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public HiveList lists_setName(String timelineId, String listId, String newName)
      throws ServiceException
  {
    Element response = invoker.invoke(new Param("method", "rtm.lists.setName"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("name", newName), new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));
    return new HiveList(response);
  }

  public void lists_unarchive()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void reflection_getMethodInfo()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void reflection_getMethods()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void settings_getList()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Adds a task, name, to the list specified by list_id.
   * @param timelineId
   * @param listId can be null to omit this parameter (assumes Inbox)
   * @param name
   * @return
   * @throws ServiceException
   */
  public HiveTaskSeries tasks_add(String timelineId, String listId, String name)
      throws ServiceException
  {
    Element response;
    if(listId != null)
        response = invoker.invoke(new Param("method", "rtm.tasks.add"), new Param("timeline", timelineId), new Param("list_id", listId),
                new Param("name", name), new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));
    else
        response = invoker.invoke(new Param("method", "rtm.tasks.add"), new Param("timeline", timelineId),
                new Param("name", name), new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));

    HiveTaskList hiveTaskList = new HiveTaskList(response);
    if (hiveTaskList.getSeries().size() == 1)
    {
      return hiveTaskList.getSeries().get(0);
    }
    else if (hiveTaskList.getSeries().size() > 1)
    {
      throw new ServiceInternalException("Internal error: more that one task (" + hiveTaskList.getSeries().size() + ") has been created");
    }
    throw new ServiceInternalException("Internal error: no task has been created");
  }

  public void tasks_addTags()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void tasks_complete(String timelineId, String listId, String taskSeriesId, String taskId)
      throws ServiceException
  {
    invoker.invoke(new Param("method", "rtm.tasks.complete"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
  }

  public void tasks_delete(String timelineId, String listId, String taskSeriesId, String taskId)
      throws ServiceException
  {
    invoker.invoke(new Param("method", "rtm.tasks.delete"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
  }

  public HiveTasks tasks_getList(String listId, String filter, Date lastSync)
      throws ServiceException
  {
    Set<Param> params = new HashSet<Param>();
    params.add(new Param("method", "rtm.tasks.getList"));
    if (listId != null)
    {
      params.add(new Param("list_id", listId));
    } else {
        // Default "To Do" Smartlist
        params.add(new Param("list_id", "1"));
    }
    if (filter != null)
    {
      params.add(new Param("filter", filter));
    }
    if (lastSync != null)
    {
      params.add(new Param("last_sync", lastSync));
    }
    params.add(new Param("auth_token", currentAuthToken));
    params.add(new Param("api_key", applicationInfo.getApiKey()));
    return new HiveTasks(invoker.invoke(params.toArray(new Param[params.size()])));
  }

  public HiveTaskSeries tasks_getTask(String taskName)
      throws ServiceException
  {
    return tasks_getTask(null, taskName);
  }

  public HiveTaskSeries tasks_getTask(String taskSeriesId, String taskName)
      throws ServiceException
  {
    Set<Param> params = new HashSet<Param>();
    params.add(new Param("method", "rtm.tasks.getList"));
    params.add(new Param("auth_token", currentAuthToken));
    params.add(new Param("api_key", applicationInfo.getApiKey()));
    params.add(new Param("filter", "name:\"" + taskName+"\""));
    HiveTasks hiveTasks = new HiveTasks(invoker.invoke(params.toArray(new Param[params.size()])));
    return findTask(taskSeriesId, hiveTasks);
  }

  private HiveTaskSeries findTask(String taskId, HiveTasks hiveTasks)
  {
    for (HiveTaskList list : hiveTasks.getLists())
    {
      for (HiveTaskSeries series : list.getSeries())
      {
        if (taskId != null)
        {
          if (series.getId().equals(taskId))
          {
            return series;
          }
        }
        else
        {
          return series;
        }
      }
    }
    return null;
  }

  public void tasks_movePriority()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public HiveTaskSeries tasks_moveTo(String timelineId, String fromListId, String toListId, String taskSeriesId, String taskId)
      throws ServiceException
  {
      if(fromListId.equals(toListId))
          return null;
    Element elt = invoker.invoke(new Param("method", "rtm.tasks.moveTo"), new Param("timeline", timelineId), new Param("from_list_id", fromListId),
        new Param("to_list_id", toListId), new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
    HiveTaskList hiveTaskList = new HiveTaskList(elt);
    return findTask(taskSeriesId, taskId, hiveTaskList);
  }

  public void tasks_postpone()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void tasks_removeTags()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void tasks_setDueDate(String timelineId, String listId, String taskSeriesId, String taskId, Date due, boolean hasDueTime)
      throws ServiceException
  {
    final boolean setDueDate = (due != null);
    if (setDueDate == true)
    {
      invoker.invoke(new Param("method", "rtm.tasks.setDueDate"), new Param("timeline", timelineId), new Param("list_id", listId),
          new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("due", due), new Param("has_due_time", hasDueTime ? "1" : "0"),
          new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));
    }
    else
    {
      invoker.invoke(new Param("method", "rtm.tasks.setDueDate"), new Param("timeline", timelineId), new Param("list_id", listId),
          new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("auth_token", currentAuthToken),
          new Param("api_key", applicationInfo.getApiKey()));
    }
  }

  public void tasks_setEstimate(String timelineId, String listId, String taskSeriesId, String taskId, String newEstimate)
          throws ServiceException
  {
      invoker.invoke(new Param("method", "rtm.tasks.setEstimate"), new Param("timeline", timelineId), new Param("list_id", listId),
              new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("estimate", newEstimate), new Param("auth_token", currentAuthToken),
              new Param("api_key", applicationInfo.getApiKey()));
  }

  public void tasks_setName(String timelineId, String listId, String taskSeriesId, String taskId, String newName)
      throws ServiceException
  {
    invoker.invoke(new Param("method", "rtm.tasks.setName"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("name", newName), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
  }

  private HiveTaskSeries findTask(String taskSeriesId, String taskId, HiveTaskList hiveTaskList)
  {
    for (HiveTaskSeries series : hiveTaskList.getSeries())
    {
      if (series.getId().equals(taskSeriesId) && series.getTask().getId().equals(taskId))
      {
        return series;
      }
    }
    return null;
  }

  public void tasks_setPriority(String timelineId, String listId, String taskSeriesId, String taskId, Priority priority)
      throws ServiceException
  {
    invoker.invoke(new Param("method", "rtm.tasks.setPriority"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("priority", HiveTask.convertPriority(priority)),
        new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));
  }

  public void tasks_setRecurrence(String timelineId, String listId, String taskSeriesId, String taskId, String repeat)
      throws ServiceException
  {
    invoker.invoke(new Param("method", "rtm.tasks.setRecurrence"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("repeat", repeat),
        new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));
  }

  public void tasks_setTags(String timelineId, String listId,
          String taskSeriesId, String taskId, String[] tags) throws ServiceException
  {
    StringBuilder tagString = new StringBuilder();
    if(tags != null) {
        for(int i = 0; i < tags.length; i++) {
            tagString.append(tags[i].replace(" ", "_"));
            if(i < tags.length - 1)
                tagString.append(",");
        }
    }
    invoker.invoke(new Param("method", "rtm.tasks.setTags"), new Param("timeline", timelineId), new Param("list_id", listId),
            new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("tags", tagString.toString()), new Param("auth_token", currentAuthToken),
            new Param("api_key", applicationInfo.getApiKey()));
  }

  public void tasks_setURL()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void tasks_uncomplete(String timelineId, String listId, String taskSeriesId, String taskId)
      throws ServiceException
  {
    invoker.invoke(new Param("method", "rtm.tasks.uncomplete"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
  }

  public HiveTaskNote tasks_notes_add(String timelineId, String listId, String taskSeriesId, String taskId, String title, String text)
      throws ServiceException
  {
    Element elt = invoker.invoke(new Param("method", "rtm.tasks.notes.add"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("note_title", title), new Param("note_text", text),
        new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));
    return new HiveTaskNote(elt);
  }

  public void tasks_notes_delete(String timelineId, String noteId)
      throws ServiceException
  {
    invoker.invoke(new Param("method", "rtm.tasks.notes.delete"), new Param("timeline", timelineId), new Param("note_id", noteId),
        new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));
  }

  public HiveTaskNote tasks_notes_edit(String timelineId, String noteId, String title, String text)
      throws ServiceException
  {
    Element elt = invoker.invoke(new Param("method", "rtm.tasks.notes.edit"), new Param("timeline", timelineId), new Param("note_id", noteId),
        new Param("note_title", title), new Param("note_text", text), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
    return new HiveTaskNote(elt);
  }

  public HiveTaskSeries tasks_setLocation(String timelineId, String listId, String taskSeriesId, String taskId, String locationId)
      throws ServiceException
  {
    Element response = invoker.invoke(new Param("method", "rtm.tasks.setLocation"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("location_id", locationId),
        new Param("auth_token", currentAuthToken), new Param("api_key", applicationInfo.getApiKey()));
    HiveTaskList hiveTaskList = new HiveTaskList(response);
    return findTask(taskSeriesId, taskId, hiveTaskList);
  }

  public HiveTaskSeries tasks_setURL(String timelineId, String listId, String taskSeriesId, String taskId, String url)
      throws ServiceException
  {
    Element response = invoker.invoke(new Param("method", "rtm.tasks.setURL"), new Param("timeline", timelineId), new Param("list_id", listId),
        new Param("taskseries_id", taskSeriesId), new Param("task_id", taskId), new Param("url", url), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
    HiveTaskList hiveTaskList = new HiveTaskList(response);
    return findTask(taskSeriesId, taskId, hiveTaskList);
  }

  public void test_echo()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void test_login()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void time_convert()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void time_parse()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public String timelines_create()
      throws ServiceException
  {
    return new HiveTimeline(invoker.invoke(new Param("method", "rtm.timelines.create"), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()))).getId();
  }

  public void timezones_getList()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void transactions_undo()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public List<HiveLocation> locations_getList()
      throws ServiceException
  {
    Element result = invoker.invoke(new Param("method", "rtm.locations.getList"), new Param("auth_token", currentAuthToken),
        new Param("api_key", applicationInfo.getApiKey()));
    List<HiveLocation> locations = new ArrayList<HiveLocation>();
    for (Element child : HiveData.children(result, "location"))
    {
      locations.add(new HiveLocation(child));
    }
    return locations;
  }

}
