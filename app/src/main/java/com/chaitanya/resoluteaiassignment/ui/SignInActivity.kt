package com.chaitanya.resoluteaiassignment.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.chaitanya.resoluteaiassignment.MainRepository
import com.chaitanya.resoluteaiassignment.R
import com.chaitanya.resoluteaiassignment.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var webRTCRepository : MainRepository
    private val firestore : FirebaseFirestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarSignInActivity)
        webRTCRepository = MainRepository.getInstance()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back)
        auth = FirebaseAuth.getInstance()
        binding.toolbarSignInActivity.setNavigationOnClickListener { finish() }
        binding.btnSignIn.setOnClickListener {
            if (binding.etEmail.text.isNullOrEmpty()){
                Toast.makeText(this@SignInActivity, "Enter Email" , Toast.LENGTH_LONG).show()

            }else if (binding.etPassword.text.isNullOrEmpty()){
                Toast.makeText(this@SignInActivity, "Enter Password" , Toast.LENGTH_LONG).show()

            }else{
                binding.btnSignIn.isEnabled = false
                auth.signInWithEmailAndPassword(binding.etEmail.text.toString(),binding.etPassword.text.toString()).addOnSuccessListener{
                    val userDocRef = firestore.collection("User").document(it.user?.uid ?: "")

                    userDocRef.get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                binding.btnSignIn.isEnabled = true
                                val userData = documentSnapshot.data

                                val name = userData?.get("name") as String
                                val email = userData["email"] as String
                                val phone = userData["phone"] as String

                                webRTCRepository.login(phone,applicationContext){
                                    finishAffinity()
                                    val intent = Intent(this, CallingActivity::class.java)
                                    intent.putExtra("USER", phone)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                                    startActivity(intent)
                                }


                            } else {
                                Log.e("Fs","fafad")
                                binding.btnSignIn.isEnabled = true
                                // User data doesn't exist in Firestore.
                                // Handle this case (e.g., show a message to the user).
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Fs",e.message.toString())
                            binding.btnSignIn.isEnabled = true
                            // Handle any errors that occur during the retrieval process.
                        }


                }.addOnFailureListener {
                    binding.btnSignIn.isEnabled = true
                    Toast.makeText(this@SignInActivity, "Invalid Credential" , Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}