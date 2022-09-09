package com.certification.l11

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.certification.l11.databinding.ActivityMainBinding
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider


class MainActivity : AppCompatActivity() {

    private val GOOGLE_SIGN_IN = 100
    private val callbackManager = CallbackManager.Factory.create()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("message", "firebase integration complete")
        analytics.logEvent("InitScreen", bundle)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Setup
        setup()
        //setSupportActionBar(binding.toolbar)

        //val navController = findNavController(R.id.nav_host_fragment_content_main)
        //appBarConfiguration = AppBarConfiguration(navController.graph)
        //setupActionBarWithNavController(navController, appBarConfiguration)

        //binding.fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show()
        //}

        session()

    }

    private fun setup() {
        title = "Authentication"
        binding.signUpButton.setOnClickListener {
            if (binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                    binding.emailEditText.text.toString(), binding.passwordEditText.text.toString()
                )
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            showHome(it.result?.user?.email ?: "", ProviderType.EMAILPASSWORD)
                        } else {
                            showAlert()
                        }
                    }
            }
        }
        binding.logInButton.setOnClickListener {
            if (binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    binding.emailEditText.text.toString(), binding.passwordEditText.text.toString()
                )
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            showHome(it.result?.user?.email ?: "", ProviderType.EMAILPASSWORD)
                        } else {
                            showAlert()
                        }
                    }

            }
        }

        binding.googleButton.setOnClickListener{
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)


            val googleIntent = Intent(this, MainActivity::class.java)

            Toast.makeText(this@MainActivity, "Google:    "+googleClient.toString(), Toast.LENGTH_LONG).show()

            //resultLauncher.launch(googleIntent)


            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)

            googleClient.signOut()
        }

        binding.facebookButton.setOnClickListener {

            LoginManager.getInstance().logInWithReadPermissions(this,  listOf("email"))

            LoginManager.getInstance().registerCallback(callbackManager,
                object :  FacebookCallback<LoginResult>{
                    override fun onSuccess(result: LoginResult) {

                        result?.let {
                            val token = it.accessToken

                            val credential = FacebookAuthProvider.getCredential(token.token)
                            Toast.makeText(this@MainActivity, "Facebook token:    "+token.token.toString(), Toast.LENGTH_LONG).show()

                            try{
                                FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener{
                                Toast.makeText(this@MainActivity, "Provider:  "+credential.provider.toString(), Toast.LENGTH_LONG).show()
                                Toast.makeText(this@MainActivity, "Signin Method:  "+credential.signInMethod.toString(), Toast.LENGTH_LONG).show()
                                Toast.makeText(this@MainActivity, "Full credential:  "+credential.toString(), Toast.LENGTH_LONG).show()
                                Toast.makeText(this@MainActivity,"getidtoken: "+it.result?.user?.getIdToken(true).toString(), Toast.LENGTH_LONG).show()


                                    val user = FirebaseAuth.getInstance().currentUser
                                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                                        val token = result.token
                                        Toast.makeText(this@MainActivity, "Firebase token:   "+token.toString(), Toast.LENGTH_LONG).show()

                                    }


                                    if (it.isSuccessful) {
                                        showHome(
                                            it.result?.user?.email ?: "",
                                            ProviderType.FACEBOOK
                                        )
                                    } else {
                                        showAlert()
                                        Toast.makeText(
                                            this@MainActivity,
                                            "no facebook instance",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }catch(e:ApiException){
                                val e1 = e
                                Toast.makeText(this@MainActivity, e1.toString(), Toast.LENGTH_LONG).show()
                                Toast.makeText(this@MainActivity, "patata", Toast.LENGTH_LONG).show()
                            }



                        }

                    }

                    override fun onCancel() {
                        TODO("Not yet implemented")
                    }

                    override fun onError(error: FacebookException) {
                        showAlert()
                    }
                })
        }

        binding.passResetButton.setOnClickListener {
            if (binding.emailEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(
                    binding.emailEditText.text.toString()
                )
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this@MainActivity, "Reset email sent", Toast.LENGTH_LONG).show()
                        } else {
                            showAlert()
                        }
                    }

            }
        }


    }


    override fun onStart() {
        super.onStart()
        binding.authLayout.visibility = View.VISIBLE
    }

    private fun session(){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email",null)
        val provider = prefs.getString("provider",null)

        if(email!=null && provider!=null){
            binding.authLayout.visibility = View.INVISIBLE
            showHome(email,ProviderType.valueOf(provider))
        }

    }

    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Error authentication user")
        builder.setPositiveButton("Ok",null)
        val dialog:AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email:String,provider:ProviderType){
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider",provider.name)
        }
        startActivity(homeIntent)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)


        if(requestCode==GOOGLE_SIGN_IN){

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Toast.makeText(this, data?.extras?.toString(), Toast.LENGTH_LONG).show()
            try {
                val account = task.getResult(ApiException::class.java)
                Toast.makeText(this, "entrando google", Toast.LENGTH_SHORT).show()
                if(account!=null){
                    val credential = GoogleAuthProvider.getCredential(account.idToken,null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if(it.isSuccessful){
                            showHome(account.email?:"" ,ProviderType.GOOGLE)
                        }else{
                            showAlert()
                        }
                    }
                }
            }catch (e: ApiException){
                showAlert()
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}