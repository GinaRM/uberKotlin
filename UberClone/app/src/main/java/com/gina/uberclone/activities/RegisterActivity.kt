package com.gina.uberclone.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.gina.uberclone.databinding.ActivityRegisterBinding
import com.gina.uberclone.models.Client
import com.gina.uberclone.providers.AuthProvider
import com.gina.uberclone.providers.ClientProvider


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authProvider = AuthProvider();
    private val clientProvider = ClientProvider();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.btnGoToLogin.setOnClickListener { goToLogin() }
        binding.btnRegister.setOnClickListener { register() }
    }

    private fun register() {
        val name = binding.textFieldName.text.toString()
        val lastname = binding.textFieldLastname.text.toString()
        val email = binding.textFieldEmail.text.toString()
        val phone = binding.textFieldPhone.text.toString()
        val password = binding.textFieldPassword.text.toString()
        val confirmPassword = binding.textFieldConfirmPassword.text.toString()

        if (isFormValid(name, lastname, email, phone, password, confirmPassword)) {
            authProvider.register(email, password).addOnCompleteListener { 
                if(it.isSuccessful) {
                    val client = Client(
                        id = authProvider.getId(),
                        name = name,
                        lastname = lastname,
                        phone = phone,
                        email = email
                    )
                    clientProvider.create(client).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this@RegisterActivity, "Successfully signed up",
                                Toast.LENGTH_LONG).show()
                            goToMap()
                        }
                        else {
                            Toast.makeText(this@RegisterActivity,
                                "There was an error while saving user data ${it.exception.toString()}",
                                Toast.LENGTH_LONG).show()
                            Log.d("FIREBASE", "Error: ${it.exception.toString()}")
                        }

                    }
                }
                else {
                    Toast.makeText(this@RegisterActivity, "Sign up failed ${it.exception.toString()}"
                        , Toast.LENGTH_LONG).show()
                    Log.d("FIREBASE", "Error: ${it.exception.toString()}")
                }
            }
        }
    }

    private fun goToMap() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun isFormValid(
        name: String,
        lastname: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if(name.isEmpty()) {
            Toast.makeText(this, "Enter your name", Toast.LENGTH_SHORT).show()
            return false
        }
        if(lastname.isEmpty()) {
            Toast.makeText(this, "Enter your lastname", Toast.LENGTH_SHORT).show()
            return false
        }
        if(phone.isEmpty()) {
            Toast.makeText(this, "Enter your phone number", Toast.LENGTH_SHORT).show()
            return false
        }
        if(email.isEmpty()) {
            Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
            return false
        }
        if(password.isEmpty()) {
            Toast.makeText(this, "Enter your password", Toast.LENGTH_SHORT).show()
            return false
        }
        if(confirmPassword.isEmpty()) {
            Toast.makeText(this, "Confirm your password", Toast.LENGTH_SHORT).show()
            return false
        }
        if(password != confirmPassword) {
            Toast.makeText(this, "The passwords must be the same", Toast.LENGTH_SHORT).show()
            return false
        }
        if(password.length <= 6) {
            Toast.makeText(this, "Password must be longer than 6 digits", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun goToLogin() {
        var i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

}