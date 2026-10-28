package io.github.staernid.sunbeam.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import io.github.staernid.sunbeam.LimeLog;

public class HelpLauncher {
    public static void launchUrl(Context context, String url) {
        if (url.startsWith("@")) {
            try {
                int resId = Integer.parseInt(url.substring(1));
                url = context.getString(resId);
            } catch (Exception ignored) {

            }
        }
        // Try to launch the default browser
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            if (!(context instanceof Activity)) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(i);
        } catch (Exception e) {
            LimeLog.warning("Unable to launch browser for " + url + ": " + e.getMessage());
        }
    }

    public static void launchSetupGuide(Context context) {
        launchUrl(context, "https://github.com/moonlight-stream/moonlight-docs/wiki/Setup-Guide");
    }

    public static void launchTroubleshooting(Context context) {
        launchUrl(context, "https://github.com/moonlight-stream/moonlight-docs/wiki/Troubleshooting");
    }

    public static void launchGameStreamEolFaq(Context context) {
        launchUrl(context, "https://github.com/moonlight-stream/moonlight-docs/wiki/NVIDIA-GameStream-End-Of-Service-Announcement-FAQ");
    }
}
