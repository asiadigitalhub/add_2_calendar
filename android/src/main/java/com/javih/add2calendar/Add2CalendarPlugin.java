package com.javih.add2calendar;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.TimeZone;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
/**
 * Add2CalendarPlugin
 **/
public class Add2CalendarPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {

    private MethodChannel channel;
    private Activity activity;
    private Context context;

    /**
     * backward compatibility with embedding v1
     **/
    @SuppressWarnings("deprecation") // Registrar deprecated (v1 plugin embedding).
    public static void registerWith(Registrar registrar) {
        Add2CalendarPlugin plugin = new Add2CalendarPlugin();
        plugin.activity = registrar.activity();
        plugin.context = registrar.context();
        plugin.setupMethodChannel(registrar.messenger());
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        setupMethodChannel(flutterPluginBinding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    private void setupMethodChannel(BinaryMessenger messenger) {
        channel = new MethodChannel(messenger, "flutter.javih.com/add_2_calendar");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        if (call.method.equals("add2Cal")) {
            try {
                result.success(insert((String) call.argument("title"),
                        (String) call.argument("desc"),
                        (String) call.argument("location"),
                        (long) call.argument("startDate"),
                        (long) call.argument("endDate"),
                        (String) call.argument("timeZone"),
                        (boolean) call.argument("allDay"),
                        (Double) call.argument("alarmInterval"),
                        (boolean) call.argument("noUI")));
            } catch (NullPointerException e) {
                result.error("Exception occurred in Android code", e.getMessage(), false);
            }
        } else {
            result.notImplemented();
        }
    }

    private boolean insert(String title, String desc, String loc, long start, long end, String timeZone, boolean allDay, Double alarm, boolean noUI) {
        if (noUI) {
            return insertNoUI(title, desc, loc, start, end, timeZone, allDay, alarm);
        }
        Context mContext = activity != null ? activity : context;
        Intent intent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE, title);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, desc);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, loc);
        intent.putExtra(CalendarContract.Events.EVENT_TIMEZONE, timeZone);
        intent.putExtra(CalendarContract.Events.EVENT_END_TIMEZONE, timeZone);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end);
        intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }

    /**
     * Adds Events and Reminders in Calendar.
     */
    private boolean insertNoUI(String title, String desc, String loc, long start, long end, String timeZone, boolean allDay, Double alarm) {
        final int callbackId = 42;

        boolean permissions = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PERMISSION_GRANTED;

        if (!permissions) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_CALENDAR,Manifest.permission.READ_CALENDAR}, callbackId);
            return false;
        }

        Context mContext = activity != null ? activity : context;
        String projection[] = {"_id", "calendar_displayName"};
        Uri calendars;
        calendars = Uri.parse("content://com.android.calendar/calendars");

        ContentResolver contentResolver = mContext.getContentResolver();
//        Cursor managedCursor = contentResolver.query(calendars, projection, null, null, null);
        String calID ="1";
        Cursor calCursor = mContext.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, projection, CalendarContract.Calendars.VISIBLE + " = 1 AND "  + CalendarContract.Calendars.IS_PRIMARY + "=1", null, CalendarContract.Calendars._ID + " ASC");

        if (calCursor.moveToFirst()){
            String calName;

            int cont= 0;
            int nameCol = calCursor.getColumnIndex(projection[1]);
            int idCol = calCursor.getColumnIndex(projection[0]);
            do {
                calName = calCursor.getString(nameCol);
                calID = calCursor.getString(idCol);
                if(calID != null && !calID.isEmpty()){
                    break;
                }
                cont++;
            } while(calCursor.moveToNext());
            calCursor.close();
        }


        TimeZone timeZone2 = TimeZone.getDefault();

        //  Calendar cal = Calendar.getInstance();
        Uri EVENTS_URI = CalendarContract.Events.CONTENT_URI; //Uri.parse(getCalendarUriBase(true) + "events");
        ContentResolver cr = mContext.getContentResolver();

        /** Inserting an event in calendar. */
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, Integer.parseInt(calID));
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.DESCRIPTION, desc);
//        values.put(CalendarContract.Events.ALL_DAY, allDay);
//        values.put(CalendarContract.Events.EVENT_LOCATION, loc);
        values.put(CalendarContract.Events.DTSTART, start);
        values.put(CalendarContract.Events.DTEND, end);
        values.put(CalendarContract.Events.HAS_ALARM, 1);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone != null ? timeZone :timeZone2.getID());

        Uri event = cr.insert(EVENTS_URI, values);

        if (alarm != null) {
            /** Adding reminder for event added. */
            values.put(CalendarContract.Events.HAS_ALARM, 1);
            values = new ContentValues();
            values.put(CalendarContract.Reminders.EVENT_ID, Long.parseLong(event.getLastPathSegment()));
            values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            values.put(CalendarContract.Reminders.MINUTES, alarm / 60);
            cr.insert(CalendarContract.Reminders.CONTENT_URI, values);
        }
        return true;
    }

}
