package com.fledgling.notiwork

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.UiThreadUtil.runOnUiThread
import com.fledgling.notiwork.models.ResponseDatum
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import java.lang.Exception
import java.net.URISyntaxException

class NotificationRequestModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    //socket.io connection url
    private var mSocket: Socket? = null

    override fun getName(): String {
        return "NotificationRequestModule"
    }

    @ReactMethod fun createNotificationRequestService(url: String, interval: Int) {
        Log.d("NotificationRequest", "Create service called with url: $url and interval ${interval.toString()}")
//        val intent = Intent(reactApplicationContext, NotificationRequestService::class.java)
//        intent.putExtra("url", url)
//        intent.putExtra("interval", interval)
//        reactApplicationContext.startService(intent)
//        setOnTimeWorkRequest(url, interval)
        try {
            //creating socket instance
            val options = IO.Options()
            options.reconnection = true //reconnection
            options.forceNew = true
            options.port = 3000
            options.path = "/unresolved-notifications-socket"
            options.secure = false

            mSocket = IO.socket(url, options)
            //mSocket = IO.socket("http://192.168.0.243:3000/", options)

            mSocket.let {
                it!!.connect().on(Socket.EVENT_CONNECT) {

                }
            }

            mSocket?.on("unresolved-notifications") { args ->
                if (args[0] != null) {
                    val responseData = args[0] as JSONArray
                    Log.i("Response Data",responseData.toString())
                    runOnUiThread {
                        // The is where you execute the actions after you receive the data
                        if (responseData != null && responseData.length() > 0) {
                            val type = object : TypeToken<List<ResponseDatum>>() {}.type
                            val dataStr = responseData.toString()
                            val modellist = Gson().fromJson<List<ResponseDatum>>(dataStr, type).toList()

                            for(NotifcationData in modellist){
                                var title = ""
                                val body = NotifcationData.eventType
                                if (NotifcationData.accessPoint != null) title = "ALERT @ ${NotifcationData.accessPoint.description}"
                                else title = "ALERT"
                                ShowNotification(title,body,"Information",NotifcationData.notificationId)
                            }
                        }
                    }
                }
            }
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    @ReactMethod fun stopNotificationRequestService() {
        Log.d("NotificationRequest", "Stop service called with NotificationRequest")
        // End a timer service
        //cancelWorkRequest()
        //reactApplicationContext.stopService(Intent(reactApplicationContext, NotificationRequestService::class.java))
        mSocket.let {
            it!!.disconnect()
        }
    }

    private fun setOnTimeWorkRequest(url:String, interval: Int) {
        try {
            if(URLUtil.isValidUrl(url) && interval > 0) {
                Log.i("MyWork", "Start")
                val myData: Data = workDataOf(
                    "url" to url,
                    "interval" to interval
                )
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val track_market = OneTimeWorkRequest.Builder(RunTask::class.java)
                    .setInputData(myData)
                    .addTag("Stock_Market")
                    .build()
                WorkManager.getInstance(reactApplicationContext).enqueue(track_market)
            }

        }catch (e: Exception){
            Log.i("MyWork", "Fail ${e.toString()}")
        }
    }

    private fun cancelWorkRequest(){
        WorkManager.getInstance(reactApplicationContext).cancelAllWork()
    }

    private fun ShowNotification(Title: String, Message: String, Information: String, id: Int) {
        val notificationManager =
            reactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "my_channel_id_01"
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Stock Market",
            NotificationManager.IMPORTANCE_HIGH
        )
        val intent = Intent(reactApplicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(reactApplicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        // Configure the notification channel.
        notificationChannel.description = "Channel description"
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.GREEN
        notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
        notificationChannel.enableVibration(true)
        notificationChannel.setSound(null, null)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(notificationChannel)
        val notificationBuilder = NotificationCompat.Builder(
            reactApplicationContext, NOTIFICATION_CHANNEL_ID
        )
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE or Notification.DEFAULT_LIGHTS)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(android.R.drawable.alert_dark_frame)
            .setSound(uri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle(Title + id)
            .setContentText(Message)
            .setContentInfo(Information)
            .setContentIntent(pendingIntent)
        notificationManager.notify( /*notification id*/id, notificationBuilder.build())
    }

}