package com.example.labexam02

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Handler // Correct import
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TaskAdapter(
    private val tasks: MutableList<Task>, // Changed to MutableList to allow modifications
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onSetTimer: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false) // Ensure this layout file exists in res/layout
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount() = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val editButton: ImageView = itemView.findViewById(R.id.buttonEdit)
        private val deleteButton: ImageView = itemView.findViewById(R.id.buttonDelete)
        private val setTimerButton: Button = itemView.findViewById(R.id.buttonSetTimer)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxDone)

        private val handler = Handler() // Use the correct Handler
        private var countdownRunnable: Runnable? = null

        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description

            // Stop any existing countdown
            countdownRunnable?.let { handler.removeCallbacks(it) }

            val timerDateTime = task.timerDateTime?.let { timerTime ->
                val currentTime = System.currentTimeMillis()
                val timeLeft = timerTime - currentTime

                if (timeLeft > 0) {
                    startCountdown(task, timeLeft)
                    formatTime(timeLeft)
                } else {
                    "Time's up!"
                }
            } ?: "No Timer Set"

            setTimerButton.text = "Timer: $timerDateTime"

            checkBox.isChecked = task.isDone
            updateTextViewStyle(task.isDone)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                task.isDone = isChecked
                updateTextViewStyle(isChecked)
                // Optionally, notify the change to your data source or database
            }

            editButton.setOnClickListener { onEdit(task) }
            deleteButton.setOnClickListener { onDelete(task) }
            setTimerButton.setOnClickListener { showDateTimePicker(task) }
        }

        private fun startCountdown(task: Task, initialTimeLeft: Long) {
            var timeLeft = initialTimeLeft

            countdownRunnable = Runnable {
                timeLeft -= 1000 // Decrease by 1 second
                if (timeLeft > 0) {
                    setTimerButton.text = "Timer: ${formatTime(timeLeft)}"
                    handler.postDelayed(countdownRunnable!!, 1000) // Schedule the next update
                } else {
                    setTimerButton.text = "Time's up!"
                    // Optionally, you can notify that the task is done
                    task.timerDateTime = null // Clear timer date/time when time is up
                }
            }
            handler.post(countdownRunnable!!) // Start the countdown
        }

        private fun formatTime(timeInMillis: Long): String {
            val hours = (timeInMillis / (1000 * 60 * 60) % 24).toInt()
            val minutes = (timeInMillis / (1000 * 60) % 60).toInt()
            val seconds = (timeInMillis / 1000 % 60).toInt()
            return String.format("%02d:%02d:%02d", hours, minutes, seconds) // Shows HH:MM:SS
        }

        private fun updateTextViewStyle(isDone: Boolean) {
            if (isDone) {
                titleTextView.paintFlags = titleTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                descriptionTextView.paintFlags = descriptionTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                titleTextView.paintFlags = titleTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                descriptionTextView.paintFlags = descriptionTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }

        private fun showDateTimePicker(task: Task) {
            val context = itemView.context
            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val timePicker = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val dateTime = Calendar.getInstance()
                            dateTime.set(year, month, dayOfMonth, hourOfDay, minute)

                            val notificationTime = dateTime.timeInMillis
                            val currentTime = System.currentTimeMillis()

                            // Calculate delay in milliseconds
                            val delay = notificationTime - currentTime

                            if (delay > 0) {
                                // Schedule the worker
                                val workRequest =
                                    OneTimeWorkRequest.Builder(TaskNotificationWorker::class.java)
                                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                        .setInputData(
                                            Data.Builder()
                                                .putString("taskTitle", task.title)
                                                .build()
                                        ).build()

                                WorkManager.getInstance(context).enqueue(workRequest)

                                // Update task timer and notify the adapter
                                task.timerDateTime = notificationTime
                                onSetTimer(task)
                            }
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    )
                    timePicker.show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }
}
