// Code Attribution
// Based on: Coroutine exceptions handling
// Author:   Kotlin
// URL:      https://kotlinlang.org/docs/exception-handling.html
// Accessed: 27 October 2026
// Used for: CoroutineExceptionHandler
//
// Based on: Use Kotlin coroutines with lifecycle-aware components
// Author:   Android Developers
// URL:      https://developer.android.com/topic/libraries/architecture/coroutines
// Accessed: 27 October 2026
// Used for: lifecycleScope.launch for database calls

package com.soo.wardensvault

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/*
launchSafe - same as lifecycleScope.launch, but if a database call fails (for example the
phone is offline) the user sees a short message instead of the app crashing.
The local Room database never failed like this; an online database can.
 */
fun AppCompatActivity.launchSafe(block: suspend CoroutineScope.() -> Unit): Job {
    val handler = CoroutineExceptionHandler { _, error ->
        Toast.makeText(this, friendlyError(error), Toast.LENGTH_LONG).show()
    }
    return lifecycleScope.launch(handler, block = block)
}

//turns a Firebase error into a message a user can understand
fun friendlyError(error: Throwable): String = when {
    error is FirebaseNetworkException -> "No internet connection"
    error is FirebaseFirestoreException &&
        error.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
        "Can't reach the database - check your internet connection"
    error is FirebaseFirestoreException &&
        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "Permission denied - check the Firestore security rules"
    else -> "Something went wrong: ${error.message}"
}
