/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.weloveastrid.hive.HiveBackgroundService;
import org.weloveastrid.hive.HiveDependencyInjector;
import org.weloveastrid.hive.HiveLoginActivity;
import org.weloveastrid.hive.HiveLoginActivity.SyncLoginCallback;
import org.weloveastrid.hive.HivePreferences;
import org.weloveastrid.hive.HiveUtilities;
import org.weloveastrid.hive.R;
import org.weloveastrid.hive.api.ApplicationInfo;
import org.weloveastrid.hive.api.ServiceImpl;
import org.weloveastrid.hive.api.ServiceInternalException;
import org.weloveastrid.hive.api.data.HiveAuth.Perms;
import org.weloveastrid.hive.api.data.HiveLists;
import org.weloveastrid.hive.api.data.HiveTask;
import org.weloveastrid.hive.api.data.HiveTask.Priority;
import org.weloveastrid.hive.api.data.HiveTaskList;
import org.weloveastrid.hive.api.data.HiveTaskNote;
import org.weloveastrid.hive.api.data.HiveTaskSeries;
import org.weloveastrid.hive.api.data.HiveTasks;
import org.weloveastrid.hive.data.HiveListService;
import org.weloveastrid.hive.data.HiveMetadataService;
import org.weloveastrid.hive.data.HiveNoteFields;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;

public class HiveSyncProvider extends SyncProvider<HiveTaskContainer> {

    private ServiceImpl hiveService = null;
    private String timeline = null;

    @Autowired private HiveMetadataService hiveMetadataService;
    @Autowired private HiveListService hiveListService;

    static {
        HiveDependencyInjector.initialize();
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- public methods
    // ----------------------------------------------------------------------

    /**
     * Sign out of Hiveminder, deleting all synchronization metadata
     */
    public void signOut(Context context) {
        ContextManager.setContext(context);
        HiveUtilities.INSTANCE.setToken(null);
        HiveUtilities.INSTANCE.clearLastSyncDate();

        DependencyInjectionService.getInstance().inject(this);
        hiveMetadataService.clearMetadata();
    }

    /**
     * Deal with a synchronization exception. If requested, will show an error
     * to the user (unless synchronization is happening in background)
     *
     * @param context
     * @param tag
     *            error tag
     * @param e
     *            exception
     * @param showError
     *            whether to display a dialog
     */
    @Override
    protected void handleException(String tag, Exception e, boolean showError) {
        final Context context = ContextManager.getContext();
        HiveUtilities.INSTANCE.setLastError(e.toString(), "");

        String message = null;

        // occurs when application was closed
        if(e instanceof IllegalStateException) {
            Log.e(tag, "caught", e); //$NON-NLS-1$

        // occurs when network error
        } else if(e instanceof ServiceInternalException &&
                ((ServiceInternalException)e).getEnclosedException() instanceof
                IOException) {
            Exception enclosedException = ((ServiceInternalException)e).getEnclosedException();
            message = context.getString(R.string.hive_ioerror);
            Log.e(tag, "ioexception", enclosedException); //$NON-NLS-1$
        } else {
            if(e instanceof ServiceInternalException)
                e = ((ServiceInternalException)e).getEnclosedException();
            if(e != null)
                message = e.toString();
            Log.e(tag, "unhandled", e); //$NON-NLS-1$
        }

        if(showError && context instanceof Activity && message != null) {
            DialogUtilities.okDialog((Activity)context, message, null);
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ initiating sync
    // ----------------------------------------------------------------------

    /**
     * set up service
     */
    @SuppressWarnings("nls")
    private void initializeService(String authToken) throws ServiceInternalException {
        String appName = null;
        // bogus values
        String z = stripslashes(0,"a2536i1534h56eru64375vn23ue3t589", "b");
        String v = stripslashes(16,"84r4h745749384ut","a");

        if(authToken == null)
            hiveService = new ServiceImpl(new ApplicationInfo(
                    z, v, appName));
        else
            hiveService = new ServiceImpl(new ApplicationInfo(
                    z, v, appName, authToken));
    }

    /**
     * initiate sync in background
     */
    @Override
    @SuppressWarnings("nls")
    protected void initiateBackground() {
        DependencyInjectionService.getInstance().inject(this);

        try {
            String authToken = HiveUtilities.INSTANCE.getToken();

            // check if we have a token & it works
            if(authToken != null) {
                initializeService(authToken);
                if(!hiveService.isServiceAuthorized()) // re-do login
                    authToken = null;
            }

            if(authToken == null) {
                // try completing the authorization if it was partial
                if(hiveService != null) {
                    try {
                        String token = hiveService.completeAuthorization();
                        HiveUtilities.INSTANCE.setToken(token);
                        performSync();

                        return;
                    } catch (Exception e) {
                        // didn't work. do the process again.
                    }
                }

                // can't do anything, user not logged in

            } else {
                performSync();
            }
        } catch (IllegalStateException e) {
            // occurs when application was closed
        } catch (Exception e) {
            handleException("hive-authenticate", e, true);
        } finally {
            HiveUtilities.INSTANCE.stopOngoing();
        }
    }

    /**
     * If user isn't already signed in, show sign in dialog. Else perform sync.
     */
    @SuppressWarnings("nls")
    @Override
    protected void initiateManual(final Activity activity) {
        final Resources r = activity.getResources();
        String authToken = HiveUtilities.INSTANCE.getToken();
        HiveUtilities.INSTANCE.stopOngoing();

        // check if we have a token & it works
        if(authToken == null) {
            // open up a dialog and have the user go to browser
            final String url;
            try {
                initializeService(null);
                url = hiveService.beginAuthorization(Perms.delete);
            } catch (Exception e) {
                handleException("hive-auth", e, true);
                return;
            }

            Intent intent = new Intent(activity, HiveLoginActivity.class);
            HiveLoginActivity.setCallback(new SyncLoginCallback() {
                public String verifyLogin(final Handler syncLoginHandler) {
                    if(hiveService == null) {
                        return null;
                    }
                    try {
                        String token = hiveService.completeAuthorization();
                        HiveUtilities.INSTANCE.setToken(token);
                        synchronize(activity);
                        return null;
                    } catch (Exception e) {
                        // didn't work
                        handleException("hive-verify-login", e, false);
                        hiveService = null;
                        if(e instanceof ServiceInternalException)
                            e = ((ServiceInternalException)e).getEnclosedException();
                        return r.getString(R.string.hive_MLA_error, e.getMessage());
                    }
                }
            });
            intent.putExtra(HiveLoginActivity.URL_TOKEN, url);
            activity.startActivityForResult(intent, 0);
        } else {
            activity.startService(new Intent(HiveBackgroundService.SYNC_ACTION, null,
                    activity, HiveBackgroundService.class));
            activity.finish();
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    protected void performSync() {
        try {
            // get Hiveminder timeline
            timeline = hiveService.timelines_create();

            // load Hiveminder lists
            HiveLists lists = hiveService.lists_getList();
            hiveListService.setLists(lists);

            // read all tasks
            ArrayList<HiveTaskContainer> remoteChanges = new ArrayList<HiveTaskContainer>();
            Date lastSyncDate = new Date(HiveUtilities.INSTANCE.getLastSyncDate());
            boolean shouldSyncIndividualLists = false;
            if(lastSyncDate.getTime() == 0)
                lastSyncDate = null; // get all unfinished tasks

            // try the quick synchronization
            try {
                Thread.sleep(1000); // throttle
                HiveTasks tasks = hiveService.tasks_getList(null, null, lastSyncDate);
                addTasksToList(tasks, remoteChanges);
            } catch (Exception e) {
//                handleException("hive-quick-sync", e, false); //$NON-NLS-1$
                handleException("hive-quick-sync", e, true); //$NON-NLS-1$
//                remoteChanges.clear();
//                shouldSyncIndividualLists = true;
                return;
            }

//            if(shouldSyncIndividualLists) {
//                for(HiveList list : lists.getLists().values()) {
//                    if(list.isSmart())
//                        continue;
//                    try {
//                        Thread.sleep(1500);
//                        HiveTasks tasks = hiveService.tasks_getList(list.getId(),
//                                null, lastSyncDate);
//                        addTasksToList(tasks, remoteChanges);
//                    } catch (Exception e) {
//                        handleException("hive-indiv-sync", e, true); //$NON-NLS-1$
//                        continue;
//                    }
//                }
//            }

            SyncData<HiveTaskContainer> syncData = populateSyncData(remoteChanges);
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            HiveUtilities.INSTANCE.recordSuccessfulSync();
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("hive-sync", e, true); //$NON-NLS-1$
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------------ sync data
    // ----------------------------------------------------------------------

    // all synchronized properties
    private static final Property<?>[] PROPERTIES = new Property<?>[] {
            Task.ID,
            Task.TITLE,
            Task.IMPORTANCE,
            Task.DUE_DATE,
            Task.CREATION_DATE,
            Task.COMPLETION_DATE,
            Task.DELETION_DATE,
            Task.NOTES,
    };

    /**
     * Populate SyncData data structure
     */
    private SyncData<HiveTaskContainer> populateSyncData(ArrayList<HiveTaskContainer> remoteTasks) {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = hiveMetadataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = hiveMetadataService.getLocallyUpdated(PROPERTIES);

        return new SyncData<HiveTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    /**
     * Add the tasks read from Hiveminder to the given list
     */
    private void addTasksToList(HiveTasks tasks, ArrayList<HiveTaskContainer> list) {
        for (HiveTaskList taskList : tasks.getLists()) {
            for (HiveTaskSeries taskSeries : taskList.getSeries()) {
                HiveTaskContainer remote = parseRemoteTask(taskSeries);

                // update reminder flags for incoming remote tasks to prevent annoying
                if(remote.task.hasDueDate() && remote.task.getValue(Task.DUE_DATE) < DateUtilities.now())
                    remote.task.setFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE, false);

                hiveMetadataService.findLocalMatch(remote);
                list.add(remote);
            }
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    @Override
    protected HiveTaskContainer create(HiveTaskContainer task) throws IOException {
        String listId = null;
        if(task.listId > 0)
            listId = Long.toString(task.listId);
        HiveTaskSeries hiveTask = hiveService.tasks_add(timeline, listId,
                task.task.getValue(Task.TITLE));
        HiveTaskContainer newRemoteTask = parseRemoteTask(hiveTask);
        transferIdentifiers(newRemoteTask, task);
        push(task, newRemoteTask);
        return newRemoteTask;
    }

    /**
     * Determine whether this task's property should be transmitted
     * @param task task to consider
     * @param property property to consider
     * @param remoteTask remote task proxy
     * @return
     */
    private boolean shouldTransmit(HiveTaskContainer task, Property<?> property, HiveTaskContainer remoteTask) {
        if(!task.task.containsValue(property))
            return false;

        if(remoteTask == null)
            return true;
        if(!remoteTask.task.containsValue(property))
            return true;

        // special cases - match if they're zero or nonzero
        if(property == Task.COMPLETION_DATE ||
                property == Task.DELETION_DATE)
            return !AndroidUtilities.equals((Long)task.task.getValue(property) == 0,
                    (Long)remoteTask.task.getValue(property) == 0);

        return !AndroidUtilities.equals(task.task.getValue(property),
                remoteTask.task.getValue(property));
    }

    /**
     * Send changes for the given Task across the wire. If a remoteTask is
     * supplied, we attempt to intelligently only transmit the values that
     * have changed.
     * @return
     */
    @Override
    protected HiveTaskContainer push(HiveTaskContainer local, HiveTaskContainer remote) throws IOException {
        boolean remerge = false;

        String listId = Long.toString(local.listId);
        String taskSeriesId = Long.toString(local.taskSeriesId);
        String taskId = Long.toString(local.taskId);

        // always update the taskname on the serverside to avoid creating a duplicate
        // because otherwise pull gets nothing from the server
        // due to the inability of RTM-API to get a task by its IDs, only by name.
        // So in case of a tasktitle-conflict, Astrid-title always takes precedence.
        hiveService.tasks_setName(timeline, listId, taskSeriesId,
                taskId, local.task.getValue(Task.TITLE));

        // fetch remote task for comparison
        if(remote == null)
            remote = pull(local);

        if(remote != null && !AndroidUtilities.equals(local.listId, remote.listId))
            hiveService.tasks_moveTo(timeline, Long.toString(remote.listId),
                    listId, taskSeriesId, taskId);

        // either delete or re-create if necessary
        if(shouldTransmit(local, Task.DELETION_DATE, remote)) {
            if(local.task.getValue(Task.DELETION_DATE) > 0)
                hiveService.tasks_delete(timeline, listId, taskSeriesId, taskId);
            else if(remote == null) {
                HiveTaskSeries hiveTask = hiveService.tasks_add(timeline, listId,
                        local.task.getValue(Task.TITLE));
                remote = parseRemoteTask(hiveTask);
                transferIdentifiers(remote, local);
            }
        }

        if(shouldTransmit(local, Task.IMPORTANCE, remote))
            hiveService.tasks_setPriority(timeline, listId, taskSeriesId,
                    taskId, Priority.values(local.task.getValue(Task.IMPORTANCE)));
        if(shouldTransmit(local, Task.DUE_DATE, remote))
            hiveService.tasks_setDueDate(timeline, listId, taskSeriesId,
                    taskId, DateUtilities.unixtimeToDate(local.task.getValue(Task.DUE_DATE)),
                    local.task.hasDueTime());
        if(shouldTransmit(local, Task.COMPLETION_DATE, remote)) {
            if(local.task.getValue(Task.COMPLETION_DATE) == 0)
                hiveService.tasks_uncomplete(timeline, listId, taskSeriesId,
                        taskId);
            else {
                hiveService.tasks_complete(timeline, listId, taskSeriesId,
                        taskId);
                // if repeating, pull and merge
                if(local.repeating)
                    remerge = true;
            }
        }

        // tags
        HashSet<String> localTags = new HashSet<String>();
        HashSet<String> remoteTags = new HashSet<String>();
        for(Metadata item : local.metadata)
            if(HiveMetadataService.TAG_KEY.equals(item.getValue(Metadata.KEY)))
                localTags.add(item.getValue(Metadata.VALUE1));
        if(remote != null && remote.metadata != null) {
            for(Metadata item : remote.metadata)
                if(HiveMetadataService.TAG_KEY.equals(item.getValue(Metadata.KEY)))
                    remoteTags.add(item.getValue(Metadata.VALUE1));
        }
        if(!localTags.equals(remoteTags)) {
            String[] tags = localTags.toArray(new String[localTags.size()]);
            hiveService.tasks_setTags(timeline, listId, taskSeriesId,
                    taskId, tags);
        }

        // notes
        if(shouldTransmit(local, Task.NOTES, remote)) {
            String[] titleAndText = HiveNoteFields.fromNoteField(local.task.getValue(Task.NOTES));
            List<HiveTaskNote> notes = null;
            if(remote != null && remote.remote.getNotes() != null)
                notes = remote.remote.getNotes().getNotes();
            if(notes != null && notes.size() > 0) {
                String remoteNoteId = notes.get(0).getId();
                hiveService.tasks_notes_edit(timeline, remoteNoteId, titleAndText[0],
                        titleAndText[1]);
            } else {
                hiveService.tasks_notes_add(timeline, listId, taskSeriesId,
                        taskId, titleAndText[0], titleAndText[1]);
            }
        }

        if(remerge) {
            remote = pull(local);
            remote.task.setId(local.task.getId());

            // transform local into remote
            local.task = remote.task;
            local.listId = remote.listId;
            local.taskId = remote.taskId;
            local.repeating = remote.repeating;
            local.taskSeriesId = remote.taskSeriesId;
        }

        return remote;
    }

    /** Create a task container for the given HiveTaskSeries */
    private HiveTaskContainer parseRemoteTask(HiveTaskSeries hiveTaskSeries) {
        Task task = new Task();
        HiveTask hiveTask = hiveTaskSeries.getTask();
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        task.setValue(Task.TITLE, hiveTaskSeries.getName());
        task.setValue(Task.CREATION_DATE, DateUtilities.dateToUnixtime(hiveTask.getAdded()));
        task.setValue(Task.COMPLETION_DATE, DateUtilities.dateToUnixtime(hiveTask.getCompleted()));
        task.setValue(Task.DELETION_DATE, DateUtilities.dateToUnixtime(hiveTask.getDeleted()));
        if(hiveTask.getDue() != null) {
            task.setValue(Task.DUE_DATE,
                    task.createDueDate(hiveTask.getHasDueTime() ? Task.URGENCY_SPECIFIC_DAY_TIME :
                        Task.URGENCY_SPECIFIC_DAY, DateUtilities.dateToUnixtime(hiveTask.getDue())));
        } else {
            task.setValue(Task.DUE_DATE, 0L);
        }
        task.setValue(Task.IMPORTANCE, hiveTask.getPriority().ordinal());

        if(hiveTaskSeries.getTags() != null) {
            for(String tag : hiveTaskSeries.getTags()) {
                Metadata tagData = new Metadata();
                tagData.setValue(Metadata.KEY, HiveMetadataService.TAG_KEY);
                tagData.setValue(Metadata.VALUE1, tag);
                metadata.add(tagData);
            }
        }

        task.setValue(Task.NOTES, ""); //$NON-NLS-1$
        if(hiveTaskSeries.getNotes() != null && hiveTaskSeries.getNotes().getNotes().size() > 0) {
            boolean firstNote = true;
            Collections.reverse(hiveTaskSeries.getNotes().getNotes()); // reverse so oldest is first
            for(HiveTaskNote note : hiveTaskSeries.getNotes().getNotes()) {
                if(firstNote) {
                    firstNote = false;
                    task.setValue(Task.NOTES, HiveNoteFields.toNoteField(note));
                } else
                    metadata.add(HiveNoteFields.create(note));
            }
        }

        HiveTaskContainer container = new HiveTaskContainer(task, metadata, hiveTaskSeries);

        return container;
    }

    @Override
    protected HiveTaskContainer pull(HiveTaskContainer task) throws IOException {
        if(task.taskSeriesId == 0)
            throw new ServiceInternalException("Tried to read an invalid task"); //$NON-NLS-1$
        HiveTaskSeries hiveTask = hiveService.tasks_getTask(Long.toString(task.taskSeriesId),
                task.task.getValue(Task.TITLE));
        if(hiveTask != null)
            return parseRemoteTask(hiveTask);
        return null;
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- read / write
    // ----------------------------------------------------------------------

    @Override
    protected HiveTaskContainer read(TodorooCursor<Task> cursor) throws IOException {
        return hiveMetadataService.readTaskAndMetadata(cursor);
    }

    @Override
    protected void write(HiveTaskContainer task) throws IOException {
        hiveMetadataService.saveTaskAndMetadata(task);
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- misc helpers
    // ----------------------------------------------------------------------

    @Override
    protected int matchTask(ArrayList<HiveTaskContainer> tasks, HiveTaskContainer target) {
        int length = tasks.size();
        for(int i = 0; i < length; i++) {
            HiveTaskContainer task = tasks.get(i);
            if(AndroidUtilities.equals(task.listId, target.listId) &&
                    AndroidUtilities.equals(task.taskSeriesId, target.taskSeriesId) &&
                    AndroidUtilities.equals(task.taskId, target.taskId))
                return i;
        }
        return -1;
    }

    @Override
    protected int updateNotification(Context context, Notification notification) {
        String notificationTitle = context.getString(R.string.hive_notification_title);
        Intent intent = new Intent(context, HivePreferences.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        notification.setLatestEventInfo(context,
                notificationTitle, context.getString(R.string.SyP_progress),
                notificationIntent);
        return 0;
    }

    @Override
    protected void transferIdentifiers(HiveTaskContainer source,
            HiveTaskContainer destination) {
        destination.listId = source.listId;
        destination.taskSeriesId = source.taskSeriesId;
        destination.taskId = source.taskId;
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- helper classes
    // ----------------------------------------------------------------------

    private static final String stripslashes(int ____,String __,String ___) {
        int _=__.charAt(____/92);_=_==115?_-1:_;_=((_>=97)&&(_<=123)?
        ((_-83)%27+97):_);return TextUtils.htmlEncode(____==31?___:
        stripslashes(____+1,__.substring(1),___+((char)_)));
    }

    @Override
    protected SyncProviderUtilities getUtilities() {
        return HiveUtilities.INSTANCE;
    }
}
