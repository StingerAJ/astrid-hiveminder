/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive;

import org.weloveastrid.hive.data.HiveListFields;
import org.weloveastrid.hive.data.HiveListService;
import org.weloveastrid.hive.data.HiveTaskFields;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;

/**
 * Exposes filters based on Hiveminder lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HiveFilterExposer extends BroadcastReceiver {

    @Autowired private HiveListService hiveListService;

    static {
        HiveDependencyInjector.initialize();
    }

    private Filter filterFromList(Context context, StoreObject list) {
        String listName = list.getValue(HiveListFields.NAME);
        String title = context.getString(R.string.hive_FEx_list_title,
                listName);
        ContentValues values = new ContentValues();
        values.put(Metadata.KEY.name, HiveTaskFields.METADATA_KEY);
        values.put(HiveTaskFields.LIST_ID.name, list.getValue(HiveListFields.REMOTE_ID));
        values.put(HiveTaskFields.TASK_SERIES_ID.name, 0);
        values.put(HiveTaskFields.TASK_ID.name, 0);
        values.put(HiveTaskFields.REPEATING.name, 0);
        Filter filter = new Filter(listName, title, new QueryTemplate().join(
                Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                        MetadataCriteria.withKey(HiveTaskFields.METADATA_KEY),
                        TaskCriteria.activeAndVisible(),
                        HiveTaskFields.LIST_ID.eq(list.getValue(HiveListFields.REMOTE_ID)))),
                values);

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);

        // if we aren't logged in, don't expose features
        if(!HiveUtilities.INSTANCE.isLoggedIn())
            return;

        DependencyInjectionService.getInstance().inject(this);

        StoreObject[] lists = hiveListService.getLists();

        // If user does not have any tags, don't show this section at all
        if(lists.length == 0)
            return;

        Filter[] listFilters = new Filter[lists.length];
        for(int i = 0; i < lists.length; i++)
            listFilters[i] = filterFromList(context, lists[i]);

        FilterListHeader hiveHeader = new FilterListHeader(context.getString(R.string.hive_FEx_header));
        FilterCategory hiveLists = new FilterCategory(context.getString(R.string.hive_FEx_list),
                listFilters);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[2];
        list[0] = hiveHeader;
        list[1] = hiveLists;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, HiveUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
