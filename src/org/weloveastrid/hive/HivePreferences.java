package org.weloveastrid.hive;

import org.weloveastrid.hive.sync.HiveSyncProvider;

import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HivePreferences extends SyncProviderPreferences {

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_hive;
    }

    @Override
    public void startSync() {
        new HiveSyncProvider().synchronize(this);
        finish();
    }

    @Override
    public void logOut() {
        new HiveSyncProvider().signOut(this);
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return HiveUtilities.INSTANCE;
    }

}