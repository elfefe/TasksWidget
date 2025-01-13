package com.elfefe.common.controller

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.simplejavamail.api.email.Email
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Properties

private val logdate: String
    get() = Instant
        .ofEpochMilli(System.currentTimeMillis())
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))

private fun logText(log: String) = "$logdate - $log\n"

fun Any.log(message: String) {
    val log = "${javaClass.simpleName}: $message"
    println(log)
    val fullLog = logText(log)
    logsFile.appendText(fullLog)
    sendMail(fullLog)
}

fun sendMail(content: String) {
    val username = "f.bou-reiff@orange.fr"
    val password = "***REMOVED-SECRET***"
    // FYI: passwords as a command arguments isn't safe
    // They go into your bash/zsh history and are visible when running ps

    val emailFrom = "f.bou-reiff@orange.fr"
    val emailTo = "felion33+taskswidget@gmail.com"

    val subject = "TasksWidget - log"

    try {
        val mailer = MailerBuilder.withSMTPServer("smtp.orange.fr", 587, username, password).buildMailer()
        mailer.sendMail(EmailBuilder.startingBlank().apply {
            from(emailFrom)
            to(emailTo)
            withSubject(subject)
            withPlainText(content)
        }.buildEmail()).get()

        println("Email sent successfully.")
    } catch (emailException: Exception) {
        println(emailException.stackTraceToString())
        logsFile.appendText(emailException.stackTraceToString())
    }
}