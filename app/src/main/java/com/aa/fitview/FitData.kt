package com.aa.fitview

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aa.fitview.DataType.*
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
import kotlin.math.roundToInt


val TAG = "TAGfitview"

enum class Period {
    MONTH, WEEK, DAY
}
enum class DataType {
    STEPS, DISTANCE, TIME, SPEED
}

class Data {
    val steps = ArrayList<Int?>()
    val distances = ArrayList<Int?>()
    val times = ArrayList<Int?>()
    val speeds = ArrayList<Float?>()
    val lengths = ArrayList<Float?>()
}

class FitData {


    companion object {
        var client: GoogleSignInClient? = null
        var account : GoogleSignInAccount? = null

        val dayData = Data()
        val weekData = Data()
        val monthData = Data()

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
                .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
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

        fun getFitData(context: Context, period: Period, dataType: com.aa.fitview.DataType) {
            var done = false
            when(period) {
                Period.DAY -> when(dataType) {
                    STEPS -> done = dayData.steps.size != 0
                    DISTANCE -> done = dayData.distances.size != 0
                    TIME -> done = dayData.times.size != 0
                    SPEED -> done = dayData.speeds.size != 0
                }
                Period.WEEK -> when(dataType) {
                    STEPS -> done = weekData.steps.size != 0
                    DISTANCE -> done = weekData.distances.size != 0
                    TIME -> done = weekData.times.size != 0
                    SPEED -> done = weekData.speeds.size != 0
                }
                Period.MONTH -> when(dataType) {
                    STEPS -> done = monthData.steps.size != 0
                    DISTANCE -> done = monthData.distances.size != 0
                    TIME -> done = monthData.times.size != 0
                    SPEED -> done = monthData.speeds.size != 0
                }
            }

            if (done)
            {
                EventBus.getDefault().post(Messages.FitResult())

                Log.d(TAG, "Data already present " + period + " " + dataType
                )
                return
            }

            if (account == null)
            {
                account = signIn(context)
            }

            var start = LocalDateTime.now()
            val end = LocalDateTime.now()

            var bucketByDays = 1

            var data: Data? = null
            when(period) {
                Period.DAY -> {
                    start = end.minusMonths(1)
                    data = dayData
                }
                Period.WEEK -> {
                    start = end.minusYears(1)
                    bucketByDays = 7
                    data = weekData
                }
                Period.MONTH -> {
                    start = end.minusYears(1)
                    bucketByDays = 30
                    data = monthData
                }
            }

            val endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond()
            val startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond()


            val readRequest = DataReadRequest.Builder()
                .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
                .bucketByTime(bucketByDays, TimeUnit.DAYS)

            when(dataType) {
                DISTANCE -> readRequest.aggregate(DataType.TYPE_DISTANCE_DELTA)
                STEPS -> readRequest.aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                TIME -> readRequest.aggregate(DataType.TYPE_MOVE_MINUTES)
                SPEED -> readRequest.aggregate(DataType.TYPE_SPEED)
            }

            Log.d(TAG, "Start request")
            Fitness.getHistoryClient(context, account)
                .readData(readRequest.build())
                .addOnSuccessListener({ response ->
                    Log.i(TAG, "Buckets = " + response.buckets.size.toString())
                    for (bucket in response.buckets) {
                        var distance: Int? = null
                        var time: Int? = null
                        var step: Int? = null
                        var speed: Float? = null

                        for (dataset in bucket.dataSets) {
                            when(dataset.dataType.name) {
                                "com.google.step_count.delta" -> {
                                    if (dataset.dataPoints.size > 0) {
                                        step = dataset.dataPoints[0].getValue(Field.FIELD_STEPS)
                                            .asInt()
                                        data.steps.add(step)
                                    } else {
                                        data.steps.add(0)
                                    }
                                }

                                "com.google.distance.delta" -> {
                                    if (dataset.dataPoints.size > 0) {
                                    distance = dataset.dataPoints[0].getValue(Field.FIELD_DISTANCE)
                                        .asFloat().roundToInt()
                                    data.distances.add(distance)
                                    } else {
                                        data.distances.add(0)
                                    }
                                }

                                "com.google.active_minutes" -> {
                                    if (dataset.dataPoints.size > 0) {
                                        time = dataset.dataPoints[0].getValue(Field.FIELD_DURATION)
                                            .asInt()
                                    }
                                    data.times.add(time)
                                }

                                "com.google.speed.summary" -> {
                                    if (dataset.dataPoints.size > 0) {
                                        speed = dataset.dataPoints[0].getValue(Field.FIELD_AVERAGE)
                                            .asFloat()
                                    }
                                    data.speeds.add(speed)
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
//                        if (distance != null && time != null) {
//                            data.speeds.add((distance.toFloat() / time).roundToInt())
//                        } else {
//                            data.speeds.add(null)
//                        }
//                        data.lengths.add(distance!!.toFloat() / step!!)

                    }
                    Log.i(TAG, "OnSuccess()")
                    Log.d(TAG, "Steps " + data.steps.toString())
                    Log.d(TAG, "Distances " + data.distances.toString())
                    Log.d(TAG, "Times " +  data.times.toString())
                    Log.d(TAG, "Speed " + data.speeds.toString())
                    Log.d(TAG, "Lenght " + data.lengths.toString())

                    EventBus.getDefault().post(Messages.FitResult())

                })
                .addOnFailureListener({ e -> Log.d(TAG, "OnFailure()", e) })
        }
    }
}