package com.viral32111.locationsharing

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

	override fun onCreate( savedInstanceState: Bundle? ) {

		super.onCreate( savedInstanceState )
		setContentView( R.layout.activity_login )

		val loginButton = findViewById<Button>( R.id.mainLoginButton )
		val backButton = findViewById<Button>( R.id.mainBackButton )

		backButton.setOnClickListener {
			finish()
			overridePendingTransition( R.anim.slide_in_from_left, R.anim.slide_out_to_right )
		}

	}

	override fun onBackPressed() {
		super.onBackPressed()
		overridePendingTransition( R.anim.slide_in_from_left, R.anim.slide_out_to_right )
	}

}
