package xyz.mustardcorp.secondscreen.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import xyz.mustardcorp.secondscreen.misc.Values;
import xyz.mustardcorp.secondscreen.services.SignBoardService;

public class KillReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();

        if (action != null && action.equals(Values.KILL_BC_ACTION)) {
            Log.e("Received", action);
            context.startService(new Intent(context, SignBoardService.class));
        }
    }
}