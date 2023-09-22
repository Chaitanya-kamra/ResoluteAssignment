package com.chaitanya.resoluteaiassignment.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.chaitanya.resoluteaiassignment.MainRepository
import com.chaitanya.resoluteaiassignment.R
import com.chaitanya.resoluteaiassignment.databinding.ActivitySignUpBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var webRTCRepository : MainRepository
    private lateinit var database : FirebaseDatabase
    private val firestore : FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSignUpActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back)
        binding.toolbarSignUpActivity.setNavigationOnClickListener { finish() }
        binding.btnSignUp.isEnabled = false
        webRTCRepository = MainRepository.getInstance()
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        binding.btnVerify.setOnClickListener {
            if (binding.etPhone.text.isNullOrEmpty()) {
                Toast.makeText(this@SignUpActivity, "Enter Mobile No. Properly", Toast.LENGTH_LONG)
                    .show()
            } else {
                val phoneNumber = binding.etPhone.text.toString()
                val nodeRef = database.reference.child(phoneNumber)

// Check if the phone number node already exists
                nodeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Toast.makeText(
                                this@SignUpActivity,
                                "Phone number already exists",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // The phone number node does not exist, proceed to store the data.
                            binding.btnVerify.text = "Sending OTP"
                            binding.btnVerify.isEnabled = false
                            binding.etPhone.isEnabled = false
                            val phone = "+91${binding.etPhone.text}"
                            val options = PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber(phone)
                                .setTimeout(120L, TimeUnit.SECONDS)
                                .setActivity(this@SignUpActivity)
                                .setCallbacks(object :
                                    PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                        signInWithPhoneAuthCredential(credential)
                                    }

                                    override fun onVerificationFailed(e: FirebaseException) {
                                        Log.d("Error Firebase", e.toString())
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "Enter Proper Number",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        binding.btnVerify.text = "Send OTP"
                                        binding.btnVerify.isEnabled = true
                                        binding.etPhone.isEnabled = true
                                    }

                                    override fun onCodeSent(
                                        verificationId: String,
                                        token: PhoneAuthProvider.ForceResendingToken
                                    ) {
                                        this@SignUpActivity.verificationId = verificationId
                                        Toast.makeText(
                                            this@SignUpActivity,
                                            "Otp Sent Successfully",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        binding.btnVerify.text = "Send OTP"
                                        binding.btnVerify.isEnabled = false
                                        binding.btnSignUp.isEnabled = true
                                        binding.etPhone.isEnabled = false
                                    }
                                })
                                .build()
                            PhoneAuthProvider.verifyPhoneNumber(options)
                        }
                    }


                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })

            }
        }
        binding.btnSignUp.setOnClickListener {
            if (binding.etName.text.isNullOrEmpty()){
                Toast.makeText(this@SignUpActivity , "Enter Name" , Toast.LENGTH_LONG).show()
            }else if (binding.etEmail.text.isNullOrEmpty()){
                Toast.makeText(this@SignUpActivity , "Enter Email" , Toast.LENGTH_LONG).show()
            }else if (binding.etPassword.text.isNullOrEmpty()){
                Toast.makeText(this@SignUpActivity , "Enter Email" , Toast.LENGTH_LONG).show()
            }else{
                binding.btnSignUp.isEnabled = false
                verifyVerificationCode(binding.etOtp.text.toString())

                auth.createUserWithEmailAndPassword(binding.etEmail.text.toString(),binding.etPassword.text.toString()).addOnSuccessListener{

                    val user = mutableMapOf<String , String>()
                    user["name"] = binding.etName.text.toString()
                    user["email"] = binding.etEmail.text.toString()
                    user["phone"] = binding.etPhone.text.toString()
                    user["password"] = binding.etPassword.text.toString()

                    firestore.collection("User").document(it.user?.uid ?: "").set(user)
                        .addOnSuccessListener {
                            // User data stored in Firestore successfully
                            // Now, store data in Realtime Database
                            val phone = binding.etPhone.text.toString()
                            webRTCRepository.login(phone, applicationContext) {
                                binding.btnSignUp.isEnabled = true
                                finishAffinity()
                                val intent = Intent(this@SignUpActivity, CallingActivity::class.java)
                                intent.putExtra("USER", phone)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                                startActivity(intent)
                            }
                        }

                }.addOnFailureListener {
                    binding.btnSignUp.isEnabled = true
                    Toast.makeText(this@SignUpActivity , "Enter Proper Email or User Exist" , Toast.LENGTH_LONG).show()

                }


            }
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
                auth.currentUser!!.delete()
                // Verification is successful, you can now proceed with the next steps


            } else {
                // Verification failed
                binding.btnSignUp.isEnabled = true
                Toast.makeText(this , "Check OTP", Toast.LENGTH_LONG).show()
            }
           }

}
}