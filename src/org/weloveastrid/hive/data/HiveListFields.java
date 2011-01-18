/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive.data;


import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.StoreObject;

/**
 * Data Model which represents a list in Hiveminder
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HiveListFields {

    /** type*/
    public static final String TYPE = "hive-list"; //$NON-NLS-1$

    // --- properties

    /** Remote ID */
    public static final LongProperty REMOTE_ID = new LongProperty(
            StoreObject.TABLE, StoreObject.ITEM.name);

    /** Name */
    public static final StringProperty NAME = StoreObject.VALUE1;

    /** Position */
    public static final IntegerProperty POSITION = new IntegerProperty(
            StoreObject.TABLE, StoreObject.VALUE2.name);

    /** Archived (0 or 1) */
    public static final IntegerProperty ARCHIVED = new IntegerProperty(
            StoreObject.TABLE, StoreObject.VALUE3.name);

}
