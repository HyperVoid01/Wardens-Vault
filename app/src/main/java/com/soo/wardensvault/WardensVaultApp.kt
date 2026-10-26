package com.soo.wardensvault

import android.app.Activity
import android.app.Application
import android.os.Bundle

/*
Application class - runs once when the app starts.
It gives every screen the same brick wall background (see WallDrawable) by setting it on
the window, which never changes size - not between screens and not when the keyboard opens.
 */
class WardensVaultApp : Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.setBackgroundDrawable(WallDrawable(activity))
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
