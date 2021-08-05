package com.aa.fitview

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import org.greenrobot.eventbus.EventBus
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

val TAG = "TAGfitview"

class Signin {


    companion object {
        var client: GoogleSignInClient? = null

        fun signIn(context: Context): GoogleSignInAccount {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("800175292223-l21i7e0ppunhk6ch1narpom6pqp3nq8u.apps.googleusercontent.com")
                .requestEmail()
                .build()

            client = GoogleSignIn.getClient(context, gso)

            val fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build()

            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                    context as Activity, // your activity
                    1, //GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                    account,
                    fitnessOptions
                )
            }

            return account
        }

        fun signOut() {
            client?.signOut()
        }

        fun getFitData(context: Context, account: GoogleSignInAccount) {
            val end = LocalDateTime.now()
            val start = end.minusWeeks(1)
            val endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond()
            val startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond()


            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

            val points = ArrayList<Int>()
            Log.d(TAG, "Start request")
            Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .addOnSuccessListener({ response ->
                    for (bucket in response.buckets) {
                        for (dataset in bucket.dataSets) {
                            if (dataset.dataPoints.size > 0) {
                                val steps =
                                    dataset.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
                                points.add(steps)
                                Log.d(TAG, steps.toString())
                            }
                        }
                    }
                    Log.i(TAG, "OnSuccess()")
                    EventBus.getDefault().post(Messages.FitResult(points))

                })
                .addOnFailureListener({ e -> Log.d(TAG, "OnFailure()", e) })
        }
    }
}