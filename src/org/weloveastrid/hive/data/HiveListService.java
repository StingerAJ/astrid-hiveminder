package org.weloveastrid.hive.data;

import java.util.Map;

import org.weloveastrid.hive.HiveDependencyInjector;
import org.weloveastrid.hive.api.data.HiveList;
import org.weloveastrid.hive.api.data.HiveLists;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.StoreObjectApiDao;
import com.todoroo.astrid.data.StoreObjectApiDao.StoreObjectCriteria;

/**
 * Service for reading and writing Milk lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HiveListService {

    static {
        HiveDependencyInjector.initialize();
    }

    private StoreObject[] lists = null;

    private final StoreObjectApiDao storeObjectDao;

    public HiveListService() {
        storeObjectDao = new StoreObjectApiDao(ContextManager.getContext());
    }

    /**
     * Reads lists
     */
    private void readLists() {
        if(lists != null)
            return;

        TodorooCursor<StoreObject> cursor = storeObjectDao.query(Query.select(StoreObject.PROPERTIES).
                where(StoreObjectCriteria.byType(HiveListFields.TYPE)).orderBy(Order.asc(HiveListFields.POSITION)));
        try {
            lists = new StoreObject[cursor.getCount()];
            for(int i = 0; i < lists.length; i++) {
                cursor.moveToNext();
                StoreObject list = new StoreObject(cursor);
                lists[i] = list;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * @return a list of lists
     */
    public StoreObject[] getLists() {
        readLists();
        return lists;
    }

    /**
     * Clears current cache of Hiveminder lists and loads data from Hiveminder into
     * database. Returns the inbox list.
     *
     * @param remoteLists
     * @return list with the name "inbox"
     */
    public StoreObject setLists(HiveLists remoteLists) {
        readLists();

        StoreObject inbox = null;
        for(Map.Entry<String, HiveList> remote : remoteLists.getLists().entrySet()) {
            if(remote.getValue().isSmart() || "All Tasks".equals(remote.getValue().getName())) //$NON-NLS-1$
                continue;

            long id = Long.parseLong(remote.getValue().getId());
            StoreObject local = null;
            for(StoreObject list : lists) {
                if(list.getValue(HiveListFields.REMOTE_ID).equals(id)) {
                    local = list;
                    break;
                }
            }

            if(local == null)
                local = new StoreObject();

            local.setValue(StoreObject.TYPE, HiveListFields.TYPE);
            local.setValue(HiveListFields.REMOTE_ID, id);
            local.setValue(HiveListFields.NAME, remote.getValue().getName());
            local.setValue(HiveListFields.POSITION, remote.getValue().getPosition());
            local.setValue(HiveListFields.ARCHIVED, remote.getValue().isArchived() ? 1 : 0);
            storeObjectDao.save(local);

            if(remote.getValue().isInbox()) {
                inbox = local;
            }
        }

        // clear list cache
        lists = null;
        return inbox;
    }

    /**
     * Get list name by list id
     * @param listId
     * @return null if no list by this id exists, otherwise list name
     */
    public String getListName(long listId) {
        readLists();
        for(StoreObject list : lists)
            if(list.getValue(HiveListFields.REMOTE_ID).equals(listId))
                return list.getValue(HiveListFields.NAME);
        return null;
    }
}
