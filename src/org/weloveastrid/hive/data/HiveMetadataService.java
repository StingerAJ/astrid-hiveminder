/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive.data;

import java.util.ArrayList;

import org.weloveastrid.hive.HiveDependencyInjector;
import org.weloveastrid.hive.HiveUtilities;
import org.weloveastrid.hive.sync.HiveTaskContainer;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncMetadataService;
import com.todoroo.astrid.sync.SyncProviderUtilities;

public final class HiveMetadataService extends SyncMetadataService<HiveTaskContainer>{

    static {
        HiveDependencyInjector.initialize();
    }

    public HiveMetadataService() {
        super(ContextManager.getContext());
    }

    @Override
    public HiveTaskContainer createContainerFromLocalTask(Task task,
            ArrayList<Metadata> metadata) {
        return new HiveTaskContainer(task, metadata);
    }

    @Override
    public Criterion getLocalMatchCriteria(HiveTaskContainer remoteTask) {
        return Criterion.and(HiveTaskFields.TASK_SERIES_ID.eq(remoteTask.taskSeriesId),
                        HiveTaskFields.TASK_ID.eq(remoteTask.taskId));
    }

    @Override
    public Criterion getMetadataCriteria() {
        return Criterion.or(MetadataCriteria.withKey(TAG_KEY),
                MetadataCriteria.withKey(HiveTaskFields.METADATA_KEY),
                MetadataCriteria.withKey(HiveNoteFields.METADATA_KEY));
    }

    @Override
    public String getMetadataKey() {
        return HiveTaskFields.METADATA_KEY;
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return HiveUtilities.INSTANCE;
    }

    /**
     * Reads task notes out of a task
     */
    public TodorooCursor<Metadata> getTaskNotesCursor(long taskId) {
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.PROPERTIES).
                where(MetadataCriteria.byTaskAndwithKey(taskId, HiveNoteFields.METADATA_KEY)));
        return cursor;
    }

    @Override
    public Criterion getMetadataWithRemoteId() {
        return HiveTaskFields.TASK_ID.gt(0);
    }

}
