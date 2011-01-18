/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.SyncAction;

/**
 * Exposes sync action
 *
 */
public class HiveSyncActionExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);

        // if we aren't logged in, don't expose sync action
        if(!HiveUtilities.INSTANCE.isLoggedIn())
            return;

        Intent syncIntent = new Intent(HiveBackgroundService.SYNC_ACTION, null,
                context, HiveBackgroundService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        SyncAction syncAction = new SyncAction(context.getString(R.string.hive_MPr_header),
                pendingIntent);

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, HiveUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, syncAction);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
