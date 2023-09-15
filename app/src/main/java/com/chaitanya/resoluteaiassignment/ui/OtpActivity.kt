package com.chaitanya.resoluteaiassignment.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.chaitanya.resoluteaiassignment.R
import com.chaitanya.resoluteaiassignment.databinding.ActivityOtpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class OtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarOtpActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back)
        binding.toolbarOtpActivity.setNavigationOnClickListener { finish() }
        verificationId = intent.getStringExtra("VERIFICATION").toString()
        auth = FirebaseAuth.getInstance()
        binding.btnVerify.setOnClickListener {
          verifyVerificationCode(binding.etOtp.toString())
        }
    }

    private fun verifyVerificationCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Verification is successful, you can now proceed with the next steps
                    Toast.makeText(this , "Successfully", Toast.LENGTH_LONG).show()


                } else {
                    // Verification failed
                }
            }

    }
}