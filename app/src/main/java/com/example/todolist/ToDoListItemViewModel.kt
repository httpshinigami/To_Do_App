package com.example.todolist

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToDoListItemViewModel(application: Application) : AndroidViewModel(application) {
    private val taskReference = FirebaseDatabase.getInstance("https://fit5046-assignment-3-5083c-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("tasks")
    private val repository: ToDoListItemRepository
    init{
        repository = ToDoListItemRepository(application)
    }
    val allToDoListItems: LiveData<List<ToDoListItem>> = repository.allToDoListItems.asLiveData()
    fun updateToDoListItem(toDoListItem: ToDoListItem) = viewModelScope.launch(Dispatchers.IO) {
        // Convert to do list item to a map (such that it can be updated by firebase)
        val toDoListItemMap = mapOf(
            "completed" to toDoListItem.completed,
            "createdAt" to toDoListItem.createdAt,
            "dueDate" to toDoListItem.dueDate,
            "friend" to toDoListItem.friend,
            "name" to toDoListItem.name,
            "tag" to toDoListItem.tag,
            "taskId" to toDoListItem.taskId,
            "userId" to toDoListItem.userId
        )
        taskReference.child(toDoListItem.taskId).updateChildren(toDoListItemMap)
        repository.update(toDoListItem)
    }
    fun deleteToDoListItem(toDoListItem: ToDoListItem) = viewModelScope.launch(Dispatchers.IO) {
        taskReference.child(toDoListItem.taskId).removeValue()
            .addOnSuccessListener {
                syncDataFromFirebase()
                makeToast("Successfully deleted item!")
            }
            .addOnFailureListener {
                // Handle the error, possibly notifying the user or logging the failure
                makeToast("Failed to deleted item!")
            }
    }
    fun syncDataFromFirebase() {
        taskReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val listOfToDoListItems = mutableListOf<ToDoListItem>()
                    for (snapshot in dataSnapshot.children) {
                        val taskId = snapshot.key.toString()
                        val userId = snapshot.child("userId").value.toString()
                        val name = snapshot.child("name").value.toString()
                        val tag = snapshot.child("tag").value.toString()
                        val dueDate = snapshot.child("dueDate").value.toString()
                        val friend = snapshot.child("friend").value.toString()
                        val completed = convertToBoolean(snapshot.child("completed").value)
                        val createdAt = snapshot.child("createdAt").value.toString().toLong()

                        listOfToDoListItems.add(ToDoListItem(taskId,userId,name,tag,dueDate,friend,completed,createdAt))
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.clearAllToDoListItems()
                        repository.insertAllToDoListItems(listOfToDoListItems)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    fun markItemAsCompleted(taskId: String) {
        taskReference.child(taskId).child("completed").setValue(true).addOnCompleteListener {
            if (it.isSuccessful) {
                syncDataFromFirebase()
                makeToast("Successfully marked item as complete!")
            } else {
                makeToast("Failed to mark item as complete!")
            }
        }
    }

    // Make a toast
    private fun makeToast(message: String) { Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show() }
}

fun convertToBoolean(value: Any?): Boolean {
    return when (value) {
        is Boolean -> value
        is String -> value.toBoolean()
        is Number -> value.toInt() != 0
        else -> false
    }
}