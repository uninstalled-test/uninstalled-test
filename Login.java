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
     * Retrieves a cursor containing the data for scheduled tasks.
     * 
     * @returns Cursor object containing all data from the specified content URI.
     */
    public Cursor getScheduledTasks() {
        Cursor count = context.getApplicationContext().getContentResolver().query(Uri.parse(ScheduledSensableContentProvider.CONTENT_URI.toString()), new String[]{"*"}, null, null, null, null);
        return count;
    }

    /**
     * The given function counts scheduled tasks and returns the result as an integer.
     * 
     * @returns The function counts the number of tasks scheduled.
     * 
     * Output: an integer indicating the total number of scheduled tasks.
     */
    public int countScheduledTasks() {
        return getScheduledTasks().getCount();
    }

    /**
     * Returns the count of pending scheduled tasks based on the specified Uri.
     * 
     * @returns The function returns a Count.
     */
    public int countPendingScheduledTasks() {
        Cursor count = context.getApplicationContext().getContentResolver().query(Uri.parse(ScheduledSensableContentProvider.CONTENT_URI + "/pending"), new String[]{"*"}, null, null, null, null);
        return count.getCount();
    }

	/**
	 * Cancels a Pending Intent and its Scheduled Service if there are no scheduled tasks.
	 * 
	 * @returns The function stopSchedulerIfNotNeeded returns true if it successfully
	 * cancels any scheduled intent using scheduler.
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
     * Adds a sensable to a scheduler by inserting it into the scheduled sensables table
     * of a database using a content provider.
     * 
     * @param scheduledSensable The `scheduledSensable` input parameter is serialized
     * into a ContentValues object and then inserted into the ScheduledSensables table
     * via the context's ContentResolver.
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
     * DELETE rows from ScheduledSensableContentProvider.CONTENT_URI based on Id of
     * scheduledsensable passed as parameter if rows are deleted then return true.
     * 
     * @param scheduledSensable DELETES. The `scheduledSensable` input parameter provides
     * the unique ID of a `ScheduledSensable` object to delete from the database table
     * using the ContentProvider's delete() method.
     * 
     * @returns DELETEs multiple rows from the table specified by CONTENT_URI and returns
     * true if rows were successfully deleted or false otherwise.
     */
    public boolean removeSensableFromScheduler(ScheduledSensable scheduledSensable) {
        int rowsDeleted = context.getContentResolver().delete(Uri.parse(ScheduledSensableContentProvider.CONTENT_URI + "/" + scheduledSensable.getId()), null, null);
        return (rowsDeleted > 0);
    }


    /**
     * Verbatim: sets pending to true and updates sender
     * 
     * @param scheduledSensable The `scheduledSensable` parameter sets the value of
     * `pending` to 1.
     * 
     * @returns UpdateSensableSender updates the sensablesender.
     */
    public boolean setSensablePending(ScheduledSensable scheduledSensable) {
        scheduledSensable.setPending(1);
        return updateSensableSender(scheduledSensable);
    }

    /**
     * Unsets the "sensible" parameter of an object named ScheduledSensible to the value
     * 0. The response will either be a result code and or true depending on if the unset
     * happened successfully and  the original parameter is upadatable or not.
     * 
     * @param scheduledSensable The `scheduledSensable` input parameter is used to set
     * the `pending` field of the `ScheduledSensable` object to zero.
     * 
     * @returns Updates the scheduled sensable's pending value to zero and returns a
     * result from updating the sensable sender.
     */
    public boolean unsetSensablePending(ScheduledSensable scheduledSensable) {
        scheduledSensable.setPending(0);
        return updateSensableSender(scheduledSensable);
    }

    /**
     * The given function updates a ScheduledSensable row or rows when it exists and
     * nothing else is specified.]
     * 
     * If the operation fails completely or partially it returns a positive number which
     * signals there is/are something to look at to find out why; for instance a failure
     * such as insufficient privileges could leave an empty updatedRowCount (which still
     * has some meaning because update did fail after all); other failures will of course
     * have only a partial fill.) The function leaves with a useful return value that
     * informs about how complete/useful its attempt at operation really was.]
     * 
     * @param scheduledSensable Scheduled sensables are serialized and inserted or updated.
     * 
     * @returns The output of the provided function is a boolean value that indicates
     * whether any rows were updated. If the update was successful and at least one row
     * was updated. the output is "true". Otherwise," false" is returned.
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
     * This private function checks if a sensable entry is a favorite of the user by
     * checking if there already exists an entry with the same sensorID. If so it updates
     * that sample and returns true if updated successfully otherwise it returns false.
     * 
     * @param scheduledSensable ScheduledSensable represents an individual sensor reading
     * scheduled for processing. In the context of the updateFavouriteIfAvailable()
     * function presented; its scheduledSensable parameter determines which sample record
     * needs to be checked against an existing set of favorite samples among users to
     * allow for updating user preference based on sensors with most updated/frequent
     * sampling times from the provider
     * 
     * @returns The function returns a boolean value indicating whether any rows were
     * updated successfully. It returns true if at least one row was updated and false otherwise.
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



