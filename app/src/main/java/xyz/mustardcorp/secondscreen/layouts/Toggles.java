package xyz.mustardcorp.secondscreen.layouts;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Method;
import java.util.ArrayList;

import xyz.mustardcorp.secondscreen.R;
import xyz.mustardcorp.secondscreen.misc.FlashlightController;
import xyz.mustardcorp.secondscreen.misc.Util;
import xyz.mustardcorp.secondscreen.services.SignBoardService;

import static android.content.Context.WIFI_SERVICE;
import static xyz.mustardcorp.secondscreen.misc.Values.AIRPLANE_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.AIRPLANE_TOGGLE;
import static xyz.mustardcorp.secondscreen.misc.Values.BLUETOOTH_TOGGLE;
import static xyz.mustardcorp.secondscreen.misc.Values.BT_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.FLASHLIGHT_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.FLASHLIGHT_TOGGLE;
import static xyz.mustardcorp.secondscreen.misc.Values.SOUND_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.SOUND_TOGGLE;
import static xyz.mustardcorp.secondscreen.misc.Values.WIFI_KEY;
import static xyz.mustardcorp.secondscreen.misc.Values.WIFI_TOGGLE;
import static xyz.mustardcorp.secondscreen.misc.Values.defaultToggleOrder;

/**
 * Similar to Android's QuickSettings, but colorable
 */

public class Toggles extends BaseLayout
{
    public static final int SIZE = 150;

    private LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(SIZE, SIZE, 1);

    private Context mContext;
    private LinearLayout mView;
    private BroadcastReceiver mWiFiBC;
    private BroadcastReceiver mSoundBC;
    private BroadcastReceiver mBluetoothBC;

    private ImageView bluetooth;
    private ImageView flash;
    private ImageView sound;
    private ImageView wifi;

    private ContentObserver mObserver;

    private final Display display;
    private boolean mFlashlightEnabled;
    private ImageView airplane;
    private BroadcastReceiver mAirplaneBC;

    private Handler mHandler;
    /**
     * Set orientations corrections, inflate views, register listeners; all the setup
     * @param context caller's context
     */
    public Toggles(Context context) {
        super(context);
        mContext = context;

        mParams.gravity = Gravity.CENTER;

        mHandler = new Handler(Looper.getMainLooper());

        mView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_toggles, null, false);
        mView.setLayoutDirection(LinearLayout.LAYOUT_DIRECTION_RTL);

        display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int rotation = display.getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            mView.setOrientation(LinearLayout.VERTICAL);
        } else {
            mView.setOrientation(LinearLayout.HORIZONTAL);
        }

        addInSetOrder();
        registerContentObservers();
        setOrientationListener();
    }

    @Override
    public LinearLayout getView() {
        return mView;
    }

    @Override
    public void onDestroy() {
        if (mWiFiBC != null) {
            try {
                mContext.unregisterReceiver(mWiFiBC);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mSoundBC != null) {
            try {
                mContext.unregisterReceiver(mSoundBC);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mBluetoothBC != null) {
            try {
                mContext.unregisterReceiver(mBluetoothBC);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mAirplaneBC != null) {
            try {
                mContext.unregisterReceiver(mAirplaneBC);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public void setOrientationListener() {
        OrientationEventListener listener = new OrientationEventListener(mContext)
        {
            private int oldRotation = display.getRotation();

            @Override
            public void onOrientationChanged(int i)
            {
                if (oldRotation != display.getRotation()) {
                    switch (display.getRotation()) {
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180:
                            setNormalOrientation();
                            break;
                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270:
                            setHorizontalOrientation();
                            break;
                    }

                    mHandler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            addInSetOrder();
                        }
                    });

                    oldRotation = display.getRotation();
                }
            }
        };
        listener.enable();
    }

    /**
     * If device is vertical
     */
    private void setNormalOrientation() {
        ViewGroup.LayoutParams params = mView.getLayoutParams();

        params.height = 160;
        params.width = 1040;

        mView.setLayoutParams(params);
        mView.setOrientation(LinearLayout.HORIZONTAL);
        mView.requestLayout();
    }

    /**
     * If device is horizontal
     */
    private void setHorizontalOrientation() {
        ViewGroup.LayoutParams params = mView.getLayoutParams();

        params.height = 1040;
        params.width = 160;

        mView.setLayoutParams(params);
        mView.setOrientation(LinearLayout.VERTICAL);
        mView.requestLayout();
    }

    /**
     * Make sure toggles are in the desired order
     */
    private void addInSetOrder() {
        ArrayList<String> set = Util.parseSavedToggleOrder(getContext(), defaultToggleOrder);

        mView.removeAllViews();

        boolean shouldReverse = display.getRotation() == Surface.ROTATION_270;

        if (shouldReverse) {
            ArrayList<String> reverseSet = new ArrayList<>();
            for (int i = set.size() - 1; i >= 0; i--) {
                reverseSet.add(set.get(i));
            }

            set = reverseSet;
        }

        for (String name : set)
        {
            switch (name)
            {
                case WIFI_TOGGLE:
                    setWiFiState();
                    break;
                case SOUND_TOGGLE:
                    setSoundState();
                    break;
                case FLASHLIGHT_TOGGLE:
                    setFlashlightState();
                    break;
                case BLUETOOTH_TOGGLE:
                    setBluetoothState();
                    break;
                case AIRPLANE_TOGGLE:
                    setAirplaneModeState();
            }
        }

        mView.invalidate();
        mView.requestLayout();
    }

    /**
     * Make sure WiFi wifi reflects the current state
     */
    private void setWiFiState() {
        final WifiManager wm = (WifiManager) mContext.getSystemService(WIFI_SERVICE);

        wifi = new ImageView(mContext);
        mView.addView(wifi, 0);
        wifi.setLayoutParams(mParams);

        if (setWiFiShown())
        {

            setWiFiColor();
            setWiFiToggleState(wifi);

            mWiFiBC = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    setWiFiToggleState(wifi);
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            intentFilter.addAction("android.net.wifi.STATE_CHANGE");

            mContext.registerReceiver(mWiFiBC, intentFilter);

            wifi.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    wm.setWifiEnabled(!wm.isWifiEnabled());
                }
            });

            wifi.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    mContext.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    return true;
                }
            });
        }
    }

    /**
     * Set WiFi toggle color
     */
    private void setWiFiColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), "wifi_color", Color.WHITE);
        wifi.setImageTintList(ColorStateList.valueOf(pref));
    }

    /**
     * Make sure toggle's image is correct
     * @param wifi toggle
     */
    private void setWiFiToggleState(ImageView wifi) {
        final WifiManager wm = (WifiManager) mContext.getSystemService(WIFI_SERVICE);

        if (wm.isWifiEnabled()) {
            wifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_4_bar_black_24dp, null));
        } else {
            wifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_off_black_24dp, null));
        }
    }

    private boolean setWiFiShown() {
        boolean enabled = Util.isToggleShown(getContext(), WIFI_TOGGLE);
        wifi.setVisibility(enabled ? View.VISIBLE : View.GONE);
        return enabled;
    }

    /**
     * Make sure sound wifi reflects correct state
     */
    private void setSoundState() {
        final AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        sound = new ImageView(mContext);
        mView.addView(sound, 0);
        sound.setLayoutParams(mParams);

        if (setSoundShown())
        {

            setSoundColor();
            setSoundToggleState(sound);

            mSoundBC = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    setSoundToggleState(sound);
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);

            mContext.registerReceiver(mSoundBC, intentFilter);

            sound.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    switch (am.getRingerMode())
                    {
                        case AudioManager.RINGER_MODE_SILENT:
                            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            break;
                        case AudioManager.RINGER_MODE_VIBRATE:
                            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            break;
                        case AudioManager.RINGER_MODE_NORMAL:
                            am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                            break;
                    }
                }
            });

            sound.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    mContext.startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
                    return true;
                }
            });
        }
    }

    /**
     * Set the color
     */
    private void setSoundColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), "sound_color", Color.WHITE);
        sound.setImageTintList(ColorStateList.valueOf(pref));
    }

    /**
     * Set the image
     * @param sound toggle
     */
    private void setSoundToggleState(ImageView sound) {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                sound.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_volume_mute_black_24dp, null));
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                sound.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_vibration_black_24dp, null));
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                sound.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_volume_up_black_24dp, null));
                break;
        }
    }

    private boolean setSoundShown() {
        boolean enabled = Util.isToggleShown(getContext(), SOUND_TOGGLE);
        sound.setVisibility(enabled ? View.VISIBLE : View.GONE);
        return enabled;
    }

    /**
     * Set proper flashlight state
     * (Flashlight implementation is buggy!)
     */
    private void setFlashlightState() {
        flash = new ImageView(mContext);
        mView.addView(flash, 0);
        flash.setLayoutParams(mParams);

        if (setFlashlightShown())
        {
            final FlashlightController controller = new FlashlightController(mContext);
            controller.addListener(new FlashlightController.FlashlightListener()
            {
                @Override
                public void onFlashlightChanged(final boolean enabled)
                {
                    if (mFlashlightEnabled != enabled)
                    {
                        mHandler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                setFlashlightToggleState(flash, enabled);
                            }
                        });
                    }
                }

                @Override
                public void onFlashlightError()
                {

                }

                @Override
                public void onFlashlightAvailabilityChanged(boolean available)
                {

                }
            });

            setFlashlightColor();
            setFlashlightToggleState(flash, false);

            flash.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (controller.isEnabled())
                    {
                        controller.setFlashlight(false);
                    } else
                    {
                        controller.setFlashlight(true);
                    }
                }
            });
        }
    }

    /**
     * Set proper flashlight color
     */
    private void setFlashlightColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), "flash_color", Color.WHITE);
        flash.setImageTintList(ColorStateList.valueOf(pref));
    }

    /**
     * Set toggle's image
     * @param flashlight toggle
     * @param enabled on or off
     */
    private void setFlashlightToggleState(ImageView flashlight, boolean enabled) {
        mFlashlightEnabled = enabled;
        if (enabled) {
            flashlight.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_flash_on_black_24dp, null));
        } else {
            flashlight.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_flash_off_black_24dp, null));
        }
    }

    private boolean setFlashlightShown() {
        boolean enabled = Util.isToggleShown(getContext(), FLASHLIGHT_TOGGLE);
        flash.setVisibility(enabled ? View.VISIBLE : View.GONE);
        return enabled;
    }

    /**
     * Set proper Bluetooth state
     */
    private void setBluetoothState() {
        bluetooth = new ImageView(mContext);
        mView.addView(bluetooth, 0);
        bluetooth.setLayoutParams(mParams);

        if (setBluetoothShown())
        {
            final BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();

            mBluetoothBC = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    setBluetoothToggleState(bluetooth);
                }
            };

            setBluetoothColor();
            setBluetoothToggleState(bluetooth);

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

            mContext.registerReceiver(mBluetoothBC, filter);

            bluetooth.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (ba.isEnabled()) ba.disable();
                    else ba.enable();
                }
            });

            bluetooth.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    mContext.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    return true;
                }
            });
        }
    }

    /**
     * Set proper Bluetooth color
     */
    private void setBluetoothColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), BT_KEY, Color.WHITE);
        bluetooth.setImageTintList(ColorStateList.valueOf(pref));
    }

    /**
     * Set toggle's image
     * @param bluetooth toggle
     */
    private void setBluetoothToggleState(ImageView bluetooth) {
        final BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();

        if (ba.isEnabled()) {
            bluetooth.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_bluetooth_black_24dp, null));
        } else {
            bluetooth.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_bluetooth_disabled_black_24dp, null));
        }
    }

    private boolean setBluetoothShown() {
        boolean enabled = Util.isToggleShown(getContext(), BLUETOOTH_TOGGLE);
        bluetooth.setVisibility(enabled ? View.VISIBLE : View.GONE);
        return enabled;
    }

    private void setAirplaneModeState() {
        airplane = new ImageView(mContext);
        mView.addView(airplane, 0);
        airplane.setLayoutParams(mParams);

        if (setAirplaneShown())
        {

            mAirplaneBC = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    setAirplaneToggleState(airplane);
                }
            };

            setAirplaneModeColor();
            setAirplaneToggleState(airplane);

            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

            mContext.registerReceiver(mAirplaneBC, filter);

            final ConnectivityManager mgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method setAirplaneMode = null;

            try
            {
                setAirplaneMode = ConnectivityManager.class.getMethod("setAirplaneMode", boolean.class);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            final Method setAirplaneModeFinal = setAirplaneMode;

            airplane.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    boolean enabled = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;

                    if (setAirplaneModeFinal != null)
                    {
                        try
                        {
                            setAirplaneModeFinal.invoke(mgr, !enabled);
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            });

            airplane.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    mContext.startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
                    return true;
                }
            });
        }
    }

    private void setAirplaneModeColor() {
        int pref = Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_KEY, Color.WHITE);
        airplane.setImageTintList(ColorStateList.valueOf(pref));
    }
    
    private void setAirplaneToggleState(ImageView airplane) {
        boolean enabled = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;

        if (enabled) {
            airplane.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_airplanemode_active_black_24dp, null));
        } else {
            airplane.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_airplanemode_inactive_black_24dp, null));
        }
    }

    private boolean setAirplaneShown() {
        boolean enabled = Util.isToggleShown(getContext(), AIRPLANE_TOGGLE);
        airplane.setVisibility(enabled ? View.VISIBLE : View.GONE);
        return enabled;
    }

    /**
     * Register {@link ContentObserver} and listen for relevant changes
     */
    private void registerContentObservers() {
        mObserver = new ContentObserver(null)
        {
            @Override
            public void onChange(boolean selfChange, final Uri uri)
            {
                mHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Uri wifi = Settings.Global.getUriFor(WIFI_KEY);
                        Uri sound = Settings.Global.getUriFor(SOUND_KEY);
                        Uri flash = Settings.Global.getUriFor(FLASHLIGHT_KEY);
                        Uri bt = Settings.Global.getUriFor(BT_KEY);
                        Uri airplane = Settings.Global.getUriFor(AIRPLANE_KEY);

                        Uri wifiShown = Settings.Global.getUriFor(WIFI_TOGGLE + "_shown");
                        Uri soundShown = Settings.Global.getUriFor(SOUND_TOGGLE + "_shown");
                        Uri flashShown = Settings.Global.getUriFor(FLASHLIGHT_TOGGLE + "_shown");
                        Uri btShown = Settings.Global.getUriFor(BLUETOOTH_TOGGLE + "_shown");
                        Uri airplaneShown = Settings.Global.getUriFor(AIRPLANE_TOGGLE + "_shown");

                        Uri toggleOrder = Settings.Global.getUriFor("toggle_order");

                        if (uri.equals(wifi)) setWiFiColor();
                        if (uri.equals(sound)) setSoundColor();
                        if (uri.equals(flash)) setFlashlightColor();
                        if (uri.equals(bt)) setBluetoothColor();
                        if (uri.equals(airplane)) setAirplaneModeColor();

                        if (uri.equals(wifiShown)
                                || uri.equals(soundShown)
                                || uri.equals(flashShown)
                                || uri.equals(btShown)
                                || uri.equals(airplaneShown)
                                || uri.equals(toggleOrder)) {
                            addInSetOrder();
                        }
                    }
                });
            }
        };

        mContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, mObserver);
    }
}
