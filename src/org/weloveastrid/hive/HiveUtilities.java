/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Constants and preferences for Hiveminder plugin
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class HiveUtilities extends SyncProviderUtilities {

    // --- constants

    /** add-on identifier */
    public static final String IDENTIFIER = "hive";

    public static final HiveUtilities INSTANCE = new HiveUtilities();

    private static final String EXPORTED_PREFS_CHECKED = IDENTIFIER + "-prefscheck";

    // --- utilities boilerplate

    private HiveUtilities() {
        // if no token is set, see if astrid has exported one
        if(getToken() == null && !Preferences.getBoolean(EXPORTED_PREFS_CHECKED, false)) {
            try {
                Context astridContext = ContextManager.getContext().createPackageContext("com.timsu.astrid", 0);
                SharedPreferences sharedPreferences = astridContext.getSharedPreferences("hive", 0);
                Editor editor = getPrefs().edit();
                if(sharedPreferences != null) {
                    String token = sharedPreferences.getString("hive_token", null);
                    long lastSyncDate = sharedPreferences.getLong("hive_last_sync", 0);

                    editor.putString(getIdentifier() + PREF_TOKEN, token);
                    editor.putLong(getIdentifier() + PREF_LAST_SYNC, lastSyncDate);
                }
                editor.putBoolean(EXPORTED_PREFS_CHECKED, true);
                editor.commit();
            } catch (Exception e) {
                // too bad
            }
        }
    }

;    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.hive_MPr_interval_key;
    }

}
