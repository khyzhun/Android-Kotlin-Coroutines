package com.sashakhyzhun.kotlincoroutines

import io.reactivex.Observable
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * @author SashaKhyzhun
 * Created on 7/12/18.
 */
class DiffApproaches {


    /** ez and naive (blocking ui thread) */
    fun fetchUserString(userId: String): String = "userString()"
    fun deserializeUser(userString: String): String = "User()"

    /** callback shit, okay, but we wanna avoid callback hell */
    fun fetchUserString2(userId: String, item: (String) -> Unit) {}
    fun deserializeUser2(userString: String, result: (String) -> Unit) {}

    /** RX, yo, but we have to admit that the code is still far from how
     * we originally wanted to write it - it is a bit more complex for the use case. */
    fun fetchUserString3(userId: String): Observable<String> = Observable.just(userId)
    fun deserializeUser3(userString: String): Observable<String> = Observable.just(userString)

    /** Coroutines (suspend, but non blocking. */
    fun fetchUserString4(userId: String): Deferred<String> = async { "" }
    fun deserializeUser4(userString: String): Deferred<String> = async { "" }


    fun showUserData(data: String) {
        // print data
    }



}