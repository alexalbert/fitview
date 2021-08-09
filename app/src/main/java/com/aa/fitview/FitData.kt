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
import kotlinx.coroutines.selects.select
import org.greenrobot.eventbus.EventBus
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

val TAG = "TAGfitview"

enum class Request {
    BYMONTH, BYDAY
}
enum class DataType {
    STEPS, DISTANCE, TIME
}

class FitData {


    companion object {
        var client: GoogleSignInClient? = null
        var account : GoogleSignInAccount? = null

        val steps = ArrayList<Int?>()
        val distances = ArrayList<Int?>()
        val times = ArrayList<Int?>()
        val speeds = ArrayList<Int?>()
        val lengths = ArrayList<Float?>()

        private fun signIn(context: Context): GoogleSignInAccount {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("800175292223-l21i7e0ppunhk6ch1narpom6pqp3nq8u.apps.googleusercontent.com")
                .requestEmail()
                .build()

            client = GoogleSignIn.getClient(context, gso)

            val fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_CADENCE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
                .build()

            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                    context as Activity,
                    1,
                    account,
                    fitnessOptions
                )
            }

            return account
        }

        fun signOut() {
            client?.signOut()
        }

        fun getFitData(context: Context, request: Request) {
            if (steps.size != 0)
            {
                EventBus.getDefault().post(Messages.FitResult())
                return
            }

            if (account == null)
            {
                account = signIn(context)
            }

            var start = LocalDateTime.now()
            val end = LocalDateTime.now()

            var bucketByDays = 1

            when(request) {
                Request.BYDAY -> {
                    start = end.minusMonths(1)
                }
                Request.BYMONTH -> {
                    start = end.minusYears(2)
                    bucketByDays = 30
                }
            }

            val endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond()
            val startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond()


            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_MOVE_MINUTES)
                .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
                .bucketByTime(bucketByDays, TimeUnit.DAYS)
                .build()

            Log.d(TAG, "Start request")
            Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .addOnSuccessListener({ response ->
                    Log.i(TAG, "Buckets = " + response.buckets.size.toString())
                    for (bucket in response.buckets) {
                        var distance: Int? = null
                        var time: Int? = null
                        var step: Int? = null

                        Log.i(TAG, "Datasets = " + bucket.dataSets.size.toString())

                        for (dataset in bucket.dataSets) {
                            when(dataset.dataType.name) {
                                "com.google.step_count.delta" -> {
                                    step = dataset.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
                                    steps.add(step)

                                }

                                "com.google.distance.delta" -> {
                                    distance = dataset.dataPoints[0].getValue(Field.FIELD_DISTANCE)
                                        .asFloat().roundToInt()
                                    distances.add(distance)
                                }

                                "com.google.active_minutes" -> {
                                    if (dataset.dataPoints.size > 0) {
                                        time = dataset.dataPoints[0].getValue(Field.FIELD_DURATION)
                                            .asInt()
                                    }
                                    times.add(time)
                                }
                            }


//                            if (dataset.dataPoints.size > 0) {
//                                val steps =
//                                    dataset.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
//                                val dist =
//                                    dataset.dataPoints[0].getValue(Field.FIELD_DISTANCE).asFloat().roundToInt()
//                                points.add(steps)
//                            }
                        }
                        if (distance != null && time != null) {
                            speeds.add((distance.toFloat() / time).roundToInt())
                        } else {
                            speeds.add(null)
                        }
                        lengths.add(distance!!.toFloat() / step!!)

                    }
                    Log.i(TAG, "OnSuccess()")
                    Log.d(TAG, "Steps " + steps.toString())
                    Log.d(TAG, "Distances " + distances.toString())
                    Log.d(TAG, "Times " +  times.toString())
                    Log.d(TAG, "Speed " + speeds.toString())
                    Log.d(TAG, "Lenght " + lengths.toString())

                    EventBus.getDefault().post(Messages.FitResult())

                })
                .addOnFailureListener({ e -> Log.d(TAG, "OnFailure()", e) })
        }
    }
}