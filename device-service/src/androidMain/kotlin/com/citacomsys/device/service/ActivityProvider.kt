package com.citacomsys.device.service

import android.app.Activity
import java.lang.ref.WeakReference

object ActivityProvider {
    private var activityRef = WeakReference<Activity>(null)

    var currentActivity: Activity?
        get() = activityRef.get()
        set(value) {
            activityRef = WeakReference(value)
        }
}
