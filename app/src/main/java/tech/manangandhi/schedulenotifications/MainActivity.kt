package tech.manangandhi.schedulenotifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import tech.manangandhi.schedulenotifications.ui.theme.ScheduleNotificationsTheme
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScheduleNotificationsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    ScheduleNotificationScreen()
                }
            }
        }
    }
}

@Composable
fun ScheduleNotificationScreen() {
    var delayMinutes by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = delayMinutes.toString(),
            onValueChange = {
                delayMinutes = it.toIntOrNull() ?: 0
            },
            label = { Text("Delay (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                Log.d("MainActivity", "Scheduling notification")
                coroutineScope.launch {
                    scheduleNotification(
                        context,
                        delayMinutes * 60 * 1000L, // Convert minutes to milliseconds
                        title,
                        message
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Schedule Notification")
        }
    }
}

fun scheduleNotification(context: Context, delayMillis: Long, title: String, message: String) {
    // Create an Intent that will be triggered when the alarm goes off
    val notificationIntent = Intent(context, NotificationReceiver::class.java)
        .putExtra("title", title)
        .putExtra("message", message)

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        notificationIntent,
        PendingIntent.FLAG_IMMUTABLE
    )

    // Schedule the alarm
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAtMillis = SystemClock.elapsedRealtime() + delayMillis + 5000
    Log.d("MainActivity", "Scheduling notification for ${triggerAtMillis} at ${SystemClock.elapsedRealtime()}")
    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent)
}


class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: ""
        val message = intent?.getStringExtra("message") ?: ""

        // check api level
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            // Create a notification channel
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("scheduled", "Scheduled", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification when the alarm goes off
        context?.let {
            Log.d("NotificationReceiver", "Showing notification")
            val builder = NotificationCompat.Builder(it, "default")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setChannelId("scheduled")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(0, builder.build())
        }
    }
}