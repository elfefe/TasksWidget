package com.elfefe.common.firebase.firestore

import androidx.compose.ui.res.useResource
import com.elfefe.common.model.Task
import com.elfefe.common.model.User
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.firestore.FirestoreOptions
import com.google.gson.Gson
import java.lang.Exception

class FirestoreApi private constructor() {
    lateinit var credentials: GoogleCredentials

    fun auth() {
        try {
            useResource("taskwidget-b17c3-6fa8ea1f5dbe.json") {
                credentials = ServiceAccountCredentials.fromStream(it).createScoped(
                    "https://www.googleapis.com/auth/cloud-platform"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun connectTasks(user: User, onUpdate: (User) -> Unit) {
        auth()

        val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
            .setProjectId(PROJECT_ID)
            .setCredentials(credentials)
            .build()
        firestoreOptions.service
            .collection(USERS)
            .document(user.email)
            .collection(CONTEXTS)
            .get().get()
            .documents.forEach { contextSnapshot ->
                contextSnapshot
                    .reference
                    .collection(TASKS)
                    .addSnapshotListener { value, error ->
                        value?.documents?.forEach { taskSnapshot ->
                            Gson().fromJson(Gson().toJson(taskSnapshot.data), Task::class.java).let { task ->
                                user.contexts
                                    .find { it.name == contextSnapshot.id }?.tasks
                                    ?.find { it.title == task.title }
                                    ?.apply {
                                        description = task.description
                                        deadline = task.deadline
                                        done = task.done
                                    }
                                    ?: user.contexts
                                        .find { it.name == contextSnapshot.id }?.tasks
                                        ?.add(task)
                                    ?: user.contexts.add(
                                        com.elfefe.common.model.Context(
                                            contextSnapshot.id,
                                            mutableListOf(task)
                                        )
                                    )
                                onUpdate(user)
                            }
                        } ?: error?.printStackTrace()
                    }
            }
    }

    companion object {
        val instance by lazy { FirestoreApi() }

        const val PROJECT_ID = "taskwidget-b17c3"

        const val USERS = "users"
        const val CONTEXTS = "Contexts"
        const val TASKS = "Tasks"
    }
}