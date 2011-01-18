/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;

public class HiveStartupReceiver extends BroadcastReceiver {

    @Override
    /** Called when device is restarted */
    public void onReceive(final Context context, Intent intent) {
        ContextManager.setContext(context);

        HiveBackgroundService.scheduleService();
        HiveUtilities.INSTANCE.stopOngoing();
    }

}
