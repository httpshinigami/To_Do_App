package com.example.todolist.LoginSignup

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.example.todolist.Navigation.Routes

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSignup(navController: NavHostController) {

    var newPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()


    // variables for validation
    val mContext = LocalContext.current
    var firstNameError by remember { mutableStateOf(false) }
    var lastNameError by remember { mutableStateOf(false) }
    var emailAddressError by remember { mutableStateOf(false) }
    var newPasswordError by remember { mutableStateOf(false) }



    // Top Bar
    TopAppBar(
        title = { Text(text = "Create Account") },
        navigationIcon = {
            IconButton(onClick = { navController.navigate("MainLogin") }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        )
    )
    Column(
        modifier = Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.height(65.dp))

        Row(modifier = Modifier.padding(0.dp)) {
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    firstNameError = it.isEmpty()
                                },
                label = { Text("First Name") },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(end = 8.dp)
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    lastNameError = it.isEmpty()
                                },
                label = { Text("Last Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = emailAddress,
            onValueChange = {
                emailAddress = it
                emailAddressError = !isValidEmail(it)
                            },
            label = { Text("Email Address")},
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        if (emailAddressError) {
            Text("Invalid email address", color = Color.Red)
        }


        OutlinedTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                newPasswordError = !isValidPassword(it)
                            },
            label = { Text("New Password")},
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        if (newPasswordError) {
            Text("Password must have a minimum length of 6 characters and cannot contain spaces", color = Color.Red)
        }


        if (message.isNotEmpty()) {
            Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        }

        Button(
            onClick = {
                // Check empty fields
                if (firstName.isNotBlank() && lastName.isNotBlank()
                    && emailAddress.isNotBlank() && newPassword.isNotBlank())
                {
                    // Validate email address and password
                    if (!emailAddressError && !newPasswordError)
                    {
                        checkEmailExists(emailAddress) { emailExists ->
                            if (!emailExists) {
                                // If email does not exist, proceed with account creation
                                loading = true
                                AuthenticationActivity().createAccount(emailAddress, newPassword, firstName, lastName) { isSuccess ->
                                    loading = false
                                    if (isSuccess) {
                                        navController.navigate(Routes.Home.value)
                                    }
                                }
                            } else {
                                // If email already exists, display an error message
                                Toast.makeText(
                                    mContext,
                                    "Email address already exists.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                }
                else {
                    Toast.makeText(
                        mContext,
                        "No fields could be empty.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            },
            modifier = Modifier
                .padding(bottom = 16.dp)
                .align(Alignment.CenterHorizontally),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Create New Account")
            }
        }
    }
}


fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
    val database = FirebaseDatabase.getInstance("https://fit5046-assignment-3-5083c-default-rtdb.asia-southeast1.firebasedatabase.app/")
    val mDatabase = database.reference
    val usersRef = mDatabase.child("users")
    val query = usersRef.orderByChild("email").equalTo(email)

    query.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // If there's at least one user with the provided email, it exists
            val emailExists = dataSnapshot.exists()
            callback(emailExists)
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.e("Firebase", "Error querying email existence: $databaseError")
            callback(false)
        }
    })

}

