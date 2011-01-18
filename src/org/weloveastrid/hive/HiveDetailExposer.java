/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive;

import org.weloveastrid.hive.data.HiveListService;
import org.weloveastrid.hive.data.HiveMetadataService;
import org.weloveastrid.hive.data.HiveNoteFields;
import org.weloveastrid.hive.data.HiveTaskFields;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;

/**
 * Exposes Task Details for Remember the Milk:
 * - Hiveminder list
 * - Hiveminder repeat information
 * - Hiveminder notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HiveDetailExposer extends BroadcastReceiver {

    public static final String DETAIL_SEPARATOR = " | "; //$NON-NLS-1$

    @Autowired private HiveMetadataService hiveMetadataService;
    @Autowired private HiveListService hiveListService;

    static {
        HiveDependencyInjector.initialize();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);

        // if we aren't logged in, don't expose features
        if(!HiveUtilities.INSTANCE.isLoggedIn())
            return;

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        boolean extended = intent.getBooleanExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
        String taskDetail = getTaskDetails(context, taskId, extended);
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, HiveUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(Context context, long id, boolean extended) {
        Metadata metadata = hiveMetadataService.getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        if(!extended) {
            long listId = metadata.getValue(HiveTaskFields.LIST_ID);
            String listName = hiveListService.getListName(listId);
            // Hiveminder list is out of date. don't display Hiveminder stuff
            if(listName == null)
                return null;

            if(listId > 0 && !"Inbox".equals(listName)) { //$NON-NLS-1$
                builder.append("<img src='silk_folder'/> ").append(listName).append(DETAIL_SEPARATOR); //$NON-NLS-1$
            }

            int repeat = metadata.getValue(HiveTaskFields.REPEATING);
            if(repeat != 0) {
                builder.append(context.getString(R.string.hive_TLA_repeat)).append(DETAIL_SEPARATOR);
            }
        }

        if(extended) {
            TodorooCursor<Metadata> notesCursor = hiveMetadataService.getTaskNotesCursor(id);
            try {
                for(notesCursor.moveToFirst(); !notesCursor.isAfterLast(); notesCursor.moveToNext()) {
                    metadata.readFromCursor(notesCursor);
                    builder.append(HiveNoteFields.toTaskDetail(metadata)).append(DETAIL_SEPARATOR);
                }
            } finally {
                notesCursor.close();
            }
        }

        if(builder.length() == 0)
            return null;
        String result = builder.toString();
        return result.substring(0, result.length() - DETAIL_SEPARATOR.length());
    }

}
