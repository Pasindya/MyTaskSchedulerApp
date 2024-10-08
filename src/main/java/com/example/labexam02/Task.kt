package com.example.labexam02

data class Task(
    var title: String,
    var description: String,
    var timerDateTime: Long? = null,
    var isDone: Boolean = false // Add this line
)

