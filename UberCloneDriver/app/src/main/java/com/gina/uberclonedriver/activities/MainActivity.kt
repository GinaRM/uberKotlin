package com.gina.uberclonedriver.activities


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.gina.uberclonedriver.databinding.ActivityMainBinding
import com.gina.uberclonedriver.providers.AuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val authProvider = AuthProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.btnRegister.setOnClickListener { goToRegister()}
        binding.btnLogin.setOnClickListener { login() }

    }

    private fun login() {
        var email = binding.textFieldEmail.text.toString()
        var password = binding.textFieldPassword.text.toString()
        if(isFormValid(email, password)) {
            authProvider.login(email, password).addOnCompleteListener {
                if(it.isSuccessful) {
                    goToMap()

                }
                else {
                    Toast.makeText(this@MainActivity,
                        "Sign In error ", Toast.LENGTH_SHORT).show()
                    Log.d("FIREBASE", "ERROR: ${it.exception.toString()}")
                }
            }
        }
    }

    private fun goToMap() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun isFormValid(email: String, password: String): Boolean {
        if(email.isEmpty()) {
            Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
            return false
        }
        if(password.isEmpty()) {
            Toast.makeText(this, "Enter your password", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun goToRegister() {
        var i = Intent(this, RegisterActivity::class.java)
        startActivity(i)
    }

    override fun onStart() {
        super.onStart()
        if(authProvider.sessionExist()) {
            goToMap()
        }
    }
}