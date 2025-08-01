package com.imxiqi.rnliveaudiostream;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class ResourceHelper {
    private static final String TAG = "ResourceHelper";

    /**
     * Get notification icon với fallback strategy
     * Priority: Custom config -> App icon -> Common drawables -> System default
     */
    public static int getNotificationIcon(Context context) {
        AudioConfig audioConfig = AudioConfig.getInstance();

        // Method 1: Từ AudioConfig (có thể set từ JS)
        int configIcon = audioConfig.getNotificationIcon();
        if (configIcon != 0 && resourceExists(context, configIcon)) {
            Log.d(TAG, "Using custom notification icon from config");
            return configIcon;
        }

        // Method 2: Sử dụng app icon
        int appIcon = getAppIcon(context);
        if (appIcon != 0) {
            Log.d(TAG, "Using app icon as notification icon");
            return appIcon;
        }

        // Method 3: System default fallback
        Log.d(TAG, "Using system default notification icon");
        return android.R.drawable.presence_audio_online;
    }

    /**
     * Get app icon từ ApplicationInfo
     */
    public static int getAppIcon(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            if (appInfo.icon != 0 && resourceExists(context, appInfo.icon)) {
                return appInfo.icon;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get app icon", e);
        }
        return 0;
    }


    /**
     * Get resource ID by name và type
     */
    public static int getResourceId(Context context, String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            return 0;
        }

        try {
            return context.getResources().getIdentifier(
                    name.trim(), type, context.getPackageName());
        } catch (Exception e) {
            Log.w(TAG, "Could not find resource: " + name, e);
            return 0;
        }
    }

    /**
     * Check nếu resource ID có tồn tại và accessible
     */
    public static boolean resourceExists(Context context, int resourceId) {
        if (resourceId == 0) {
            return false;
        }

        try {
            // Try to get resource name - sẽ throw exception nếu không tồn tại
            String resourceName = context.getResources().getResourceName(resourceId);

            // Try to get drawable - kiểm tra có thể access được không
            context.getResources().getDrawable(resourceId, null);

            return true;
        } catch (Exception e) {
            Log.w(TAG, "Resource not accessible: " + resourceId, e);
            return false;
        }
    }

    /**
     * Get resource name từ ID (for debugging)
     */
    public static String getResourceName(Context context, int resourceId) {
        try {
            return context.getResources().getResourceName(resourceId);
        } catch (Exception e) {
            return "unknown_resource_" + resourceId;
        }
    }
}
