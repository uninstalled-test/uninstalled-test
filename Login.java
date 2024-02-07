

package io.sensable.client.scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import io.sensable.client.sqlite.SavedSensablesTable;
import io.sensable.client.sqlite.ScheduledSensableContentProvider;
import io.sensable.client.sqlite.ScheduledSensablesTable;
import io.sensable.client.sqlite.SensableContentProvider;
import io.sensable.model.ScheduledSensable;
import io.sensable.model.Sensable;

public class ScheduleHelper {

	private static final String TAG = ScheduleHelper.class.getSimpleName();
	private Context context;
	private AlarmManager scheduler;
	private static final int PENDING_INTENT_ID = 12345;

	public ScheduleHelper(Context context) {
    	this.context = context.getApplicationContext();
		scheduler = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

	/**
	 * This function checks if the AlarmManager is already running and if it isn't creates
	 * a scheduled task to run the ScheduledSensableService every 15 minutes using PendingIntent.
	 */
	public void startScheduler() {
        Intent intent = new Intent(context.getApplicationContext(), ScheduledSensableService.class);
        boolean alarmUp = (PendingIntent.getBroadcast(context.getApplicationContext(), PENDING_INTENT_ID, intent, PendingIntent.FLAG_NO_CREATE) != null);

        if (alarmUp) {
            Log.d(TAG, "AlarmManager already running. Exit without recreating it.");
        } else {
            // Create scheduled task if it doesn't already exist.
            Log.d(TAG, "AlarmManager not running. Create it now.");
            PendingIntent scheduledIntent = PendingIntent.getService(context.getApplicationContext(), PENDING_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, scheduledIntent);
        }
    }

    /**
     * Retrieves a Cursor object containing information about all scheduled tasks. The
     * query retrieves all data from the Scheduled Sensable Content Provider using the
     * Content Resolver of the context's application.
     * 
     * @returns Cursor object containing all data from scheduledtasks.
     */
    public Cursor getScheduledTasks() {
        Cursor count = context.getApplicationContext().getContentResolver().query(Uri.parse(ScheduledSensableContentProvider.CONTENT_URI.toString()), new String[]{"*"}, null, null, null, null);
        return count;
    }

    /**
     * Returns the number of scheduled tasks using method 'getCount()' of list 'getScheduledTasks()'.
     * 
     * @returns The output returned by this function is a integer value representing the
     * number of scheduled tasks.
     */
    public int countScheduledTasks() {
        return getScheduledTasks().getCount();
    }

    /**
     * Return the count of pending scheduled tasks based on the provided Content Uri.
     * 
     * @returns The output of the function is a integer representing the number of pending
     * scheduled tasks available.
     */
    public int countPendingScheduledTasks() {
        Cursor count = context.getApplicationContext().getContentResolver().query(Uri.parse(ScheduledSensableContentProvider.CONTENT_URI + "/pending"), new String[]{"*"}, null, null, null, null);
        return count.getCount();
    }

	/**
	 * Cancels a Scheduled Sensable Service if there are no scheduled tasks present.
	 * 
	 * @returns The function returns true.
	 */
	public boolean stopSchedulerIfNotNeeded() {
    	if (countScheduledTasks() == 0) {
            Intent intent = new Intent(context, ScheduledSensableService.class);
            PendingIntent scheduledIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            scheduler.cancel(scheduledIntent);
        }
        return true;
    }


    /**
     * Adds a scheduled sensable to a database based on a serialized ScheduledSensable
     * object given as parameter. The ContentResolver of a context inserts the object's
     * values into the SQL lite dictionary content URI.
     * 
     * @param scheduledSensable ScheduledSensable is deserialized and then inserted into
     * a Content Provider
     * 
     * @returns The function returns a true value.
     */
    public boolean addSensableToScheduler(ScheduledSensable scheduledSensable) {
        ContentValues mNewValues = ScheduledSensablesTable.serializeScheduledSensableForSqlLite(scheduledSensable);

        Uri mNewUri = context.getContentResolver().insert(
                ScheduledSensableContentProvider.CONTENT_URI,   // the user dictionary content URI
                mNewValues                          // the values to insert
        );
        return true;
    }

    /**
     * Deletes rows from the database based on the content URI and the sensable's ID.
     * 
     * @param scheduledSensable The scheduledSensable input parameter passes a ScheduledSensable
     * object to be removed from the scheduler.
     * 
     * @returns Deletes rows from a specified URI based on a provided ID and returns a
     * boolean value indicating the result of the delete operation: true if at least one
     * row was deleted and false otherwise.
     */
    public boolean removeSensableFromScheduler(ScheduledSensable scheduledSensable) {
        int rowsDeleted = context.getContentResolver().delete(Uri.parse(ScheduledSensableContentProvider.CONTENT_URI + "/" + scheduledSensable.getId()), null, null);
        return (rowsDeleted > 0);
    }


    /**
     * Sets the pending status of a ScheduledSensable object to true and updates the
     * sensable sender if the update was successful.
     * 
     * @param scheduledSensable The `scheduledSensable` parameter sets the value of the
     * "pending" field to 1.
     * 
     * @returns The output returned is a boolean value indicating whether the update was
     * successful.
     */
    public boolean setSensablePending(ScheduledSensable scheduledSensable) {
        scheduledSensable.setPending(1);
        return updateSensableSender(scheduledSensable);
    }

    /**
     * Certainly. Here is the documentation you requested for the code above:
     * 
     * Unsets the 'pending' flag of the given ScheduledSensable.
     * Then updates its Sender according to the return value of updateSensableSender(scheduledSensable).
     * 
     * @param scheduledSensable Sets the value of `pending` to 0 for `ScheduledSensable`
     * object reference passed as an argument before performing the actual task that the
     * method describes.
     * 
     * @returns The method returns a boolean value representing the outcome of the operation.
     */
    public boolean unsetSensablePending(ScheduledSensable scheduledSensable) {
        scheduledSensable.setPending(0);
        return updateSensableSender(scheduledSensable);
    }

    /**
     * Updates a scheduled sensable into a content provider with serialized values and
     * sets the favourite copy if necessary.
     * 
     * @param scheduledSensable The `scheduledSensable` input parameter is serialized
     * into a ContentValues object to be updated or inserted into a database using the
     * SQL Lite Content Provider.
     * 
     * @returns The output returned by this function is a boolean value indicating whether
     * any rows were updated. Specifically 1 if at least one row was updated and 0 otherwise.
     */
    private boolean updateSensableSender(ScheduledSensable scheduledSensable) {
        ContentValues mNewValues = ScheduledSensablesTable.serializeScheduledSensableForSqlLite(scheduledSensable);

        Uri updateUri = Uri.parse(ScheduledSensableContentProvider.CONTENT_URI + "/" + scheduledSensable.getId());

        int rowsUpdated = context.getContentResolver().update(
                updateUri,   // the user dictionary content URI
                mNewValues,                          // the values to insert
                null,
                new String[]{}
        );
        // Copy this sample over to the favourite object if there is one
        updateFavouriteIfAvailable(scheduledSensable);

        return rowsUpdated > 0;
    }


    /**
     * Updates a favorite sample from the current schedules sample if available.
     * 
     * @param scheduledSensable Provides the Sensable data to be checked for availability
     * and possible updating of a favourite sample.
     * 
     * @returns The function updates a favorite sample. It returns true if at least one
     * row was updated and false otherwise.
     */
    private boolean updateFavouriteIfAvailable(ScheduledSensable scheduledSensable) {

        Uri favouriteUri = Uri.parse(SensableContentProvider.CONTENT_URI + "/" + scheduledSensable.getSensorid());
        Cursor count = context.getContentResolver().query(favouriteUri, new String[]{"*"}, null, null, null, null);

        // If this is also favourited
        if(count.getCount() > 0) {
            Sensable sensable = new Sensable();
            sensable.setSensorid(scheduledSensable.getSensorid());
            sensable.setSample(scheduledSensable.getSample());
            ContentValues mNewValues = SavedSensablesTable.serializeSensableWithSingleSampleForSqlLite(sensable);
            //Update the favourite sample
            int rowsUpdated = context.getContentResolver().update(
                    favouriteUri,   // the user dictionary content URI
                    mNewValues,                          // the values to insert
                    null,
                    new String[]{}
            );
            return rowsUpdated > 0;
        } else {
            return false;
        }

    }

}



