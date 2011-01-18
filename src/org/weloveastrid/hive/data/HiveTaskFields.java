package org.weloveastrid.hive.data;

import org.weloveastrid.hive.sync.HiveTaskContainer;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for a Hiveminder Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HiveTaskFields {

    /** metadata key */
    public static final String METADATA_KEY = "hive"; //$NON-NLS-1$

    /** {@link HiveListFields} id */
    public static final LongProperty LIST_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** Hiveminder Task Series Id */
    public static final LongProperty TASK_SERIES_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** Hiveminder Task Id */
    public static final LongProperty TASK_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** Whether task repeats in Hiveminder (1 or 0) */
    public static final IntegerProperty REPEATING = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    /**
     * Creates a piece of metadata from a remote task
     * @param hiveTaskSeries
     * @return
     */
    public static Metadata create(HiveTaskContainer container) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, METADATA_KEY);
        metadata.setValue(HiveTaskFields.LIST_ID, container.listId);
        metadata.setValue(HiveTaskFields.TASK_SERIES_ID, container.taskSeriesId);
        metadata.setValue(HiveTaskFields.TASK_ID, container.taskId);
        metadata.setValue(HiveTaskFields.REPEATING, container.repeating ? 1 : 0);

        return metadata;
    }

}
