package org.weloveastrid.hive.sync;

import java.util.ArrayList;
import java.util.Iterator;

import org.weloveastrid.hive.api.data.HiveTaskSeries;
import org.weloveastrid.hive.data.HiveTaskFields;

import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncContainer;

/**
 * Hiveminder Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HiveTaskContainer extends SyncContainer {
    public long listId, taskSeriesId, taskId;
    public boolean repeating;
    public HiveTaskSeries remote;

    public HiveTaskContainer(Task task, ArrayList<Metadata> metadata,
            long listId, long taskSeriesId, long taskId, boolean repeating,
            HiveTaskSeries remote) {
        this.task = task;
        this.metadata = metadata;
        this.listId = listId;
        this.taskSeriesId = taskSeriesId;
        this.taskId = taskId;
        this.repeating = repeating;
        this.remote = remote;
    }

    public HiveTaskContainer(Task task, ArrayList<Metadata> metadata,
            HiveTaskSeries hiveTaskSeries) {
//        this(task, metadata, Long.parseLong(hiveTaskSeries.getList().getId()),
//                Long.parseLong(hiveTaskSeries.getId()), Long.parseLong(hiveTaskSeries.getTask().getId()),
//                hiveTaskSeries.hasRecurrence(), hiveTaskSeries);
        this(task, metadata, 0,
                Long.parseLong(hiveTaskSeries.getId()), Long.parseLong(hiveTaskSeries.getTask().getId()),
                hiveTaskSeries.hasRecurrence(), hiveTaskSeries);
    }

    public HiveTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this(task, metadata, 0, 0, 0, false, null);
        for(Iterator<Metadata> iterator = metadata.iterator(); iterator.hasNext(); ) {
            Metadata item = iterator.next();
            if(HiveTaskFields.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                if(item.containsNonNullValue(HiveTaskFields.LIST_ID))
                    listId = item.getValue(HiveTaskFields.LIST_ID);
                if(item.containsNonNullValue(HiveTaskFields.TASK_SERIES_ID))
                    taskSeriesId = item.getValue(HiveTaskFields.TASK_SERIES_ID);
                if(item.containsNonNullValue(HiveTaskFields.TASK_ID))
                    taskId = item.getValue(HiveTaskFields.TASK_ID);
                if(item.containsNonNullValue(HiveTaskFields.REPEATING))
                    repeating = item.getValue(HiveTaskFields.REPEATING) == 1;
                iterator.remove();
                break;
            }
        }
    }

    @Override
    public void prepareForSaving() {
        super.prepareForSaving();
        metadata.add(HiveTaskFields.create(this));
    }

}