package com.example.labexam02


import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.app.TimePickerDialog
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {


    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    private lateinit var buttonStartTimer: Button
    private lateinit var buttonSetReminder: Button

    private val CHANNEL_ID = "task_reminder_channel"

    private val handler = Handler(Looper.getMainLooper())
    private val checkTaskTimersRunnable = object : Runnable {
        override fun run() {
            checkTaskTimers()
            handler.postDelayed(this, 60000) // Check every minute
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE)

        setupRecyclerView()
        loadTasks()




        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            addTask()
        }





        createNotificationChannel()
        handler.post(checkTaskTimersRunnable)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TaskReminderChannel"
            val descriptionText = "Channel for Task Reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)



        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(title.hashCode(), notificationBuilder.build())
    }

    private fun sendReminderForFirstTask() {
        if (taskList.isNotEmpty()) {
            val task = taskList[0] // Assuming you want to remind about the first task
            sendNotification("Task Reminder", "Reminder for task: ${task.title}")
        } else {
            Toast.makeText(this, "No tasks available to remind!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkTaskTimers() {
        val currentTime = System.currentTimeMillis()
        for (task in taskList) {
            task.timerDateTime?.let { timerTime ->
                if (currentTime >= timerTime) {
                    task.timerDateTime = null // Reset timer
                    sendNotification("Task Reminder", "Time's up for task: ${task.title}")
                }
            }
        }
        taskAdapter.notifyDataSetChanged()
        saveTasks()
    }



    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60)) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(taskList, ::editTask, ::deleteTask, ::setTaskTimer)
        recyclerView.adapter = taskAdapter
    }
    //store data in sharedPreference
    private fun saveTasks() {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(taskList)
        editor.putString("task_list", json)
        editor.apply()
    }
   //Load data from sharedPreference
    private fun loadTasks() {
        val json = sharedPreferences.getString("task_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Task>>() {}.type
            taskList.clear()
            taskList.addAll(gson.fromJson(json, type))
            taskAdapter.notifyDataSetChanged()
        }
    }

    private fun showDateTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                val dateTime = Calendar.getInstance()
                dateTime.set(year, month, dayOfMonth, hourOfDay, minute)
                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                editText.setText(dateTimeFormat.format(dateTime.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePicker.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.show()
    }

    private fun addTask() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleInput: EditText = dialogView.findViewById(R.id.editTextTaskTitle)
        val descriptionInput: EditText = dialogView.findViewById(R.id.editTextTaskDescription)
        val dateTimeInput: EditText = dialogView.findViewById(R.id.editTextTimerDateTime)

        dateTimeInput.setOnClickListener {
            showDateTimePicker(dateTimeInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                val dateTimeString = dateTimeInput.text.toString()

                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val timerDateTime: Long? = try {
                    dateTimeFormat.parse(dateTimeString)?.time
                } catch (e: Exception) {
                    null
                }

                val task = Task(title, description, timerDateTime)
                taskList.add(task)
                taskAdapter.notifyDataSetChanged()
                saveTasks()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editTask(task: Task) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleInput: EditText = dialogView.findViewById(R.id.editTextTaskTitle)
        val descriptionInput: EditText = dialogView.findViewById(R.id.editTextTaskDescription)
        val dateTimeInput: EditText = dialogView.findViewById(R.id.editTextTimerDateTime)

        titleInput.setText(task.title)
        descriptionInput.setText(task.description)
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dateTimeInput.setText(task.timerDateTime?.let { dateTimeFormat.format(Date(it)) })

        dateTimeInput.setOnClickListener {
            showDateTimePicker(dateTimeInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                task.title = titleInput.text.toString()
                task.description = descriptionInput.text.toString()
                task.timerDateTime = try {
                    dateTimeFormat.parse(dateTimeInput.text.toString())?.time
                } catch (e: Exception) {
                    null
                }
                taskAdapter.notifyDataSetChanged()
                saveTasks()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setTaskTimer(task: Task) {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateTimeString = dateTimeFormat.format(Date(task.timerDateTime ?: return))

        val dialogView = layoutInflater.inflate(R.layout.dialog_set_timer, null)
        val dateTimeInput: EditText = dialogView.findViewById(R.id.editTextTimerDateTime)
        dateTimeInput.setText(dateTimeString)

        AlertDialog.Builder(this)
            .setTitle("Set Timer for ${task.title}")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                task.timerDateTime = try {
                    dateTimeFormat.parse(dateTimeInput.text.toString())?.time
                } catch (e: Exception) {
                    null
                }
                taskAdapter.notifyDataSetChanged()
                saveTasks()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun deleteTask(task: Task) {
        // Remove the task from the list
        taskList.remove(task)
        // Notify the adapter of the changes
        taskAdapter.notifyDataSetChanged()
        // Save the updated task list
        saveTasks()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkTaskTimersRunnable)
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val title = intent.getStringExtra("title") ?: "Task Reminder"
            val message = intent.getStringExtra("message") ?: "Time's up for your task!"

            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val notificationBuilder = NotificationCompat.Builder(context, "task_reminder_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            notificationManager.notify(title.hashCode(), notificationBuilder.build())
        }
    }
}
