package com.viral32111.locationsharing

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.launch

class ServerActivity : AppCompatActivity() {

	// Define constants
	private val logTag = "LocationSharing" // TODO: Move this to shared class
	private val defaultServerUrl = "https://locationsharing.viral32111.cf"

	// Setup the Ktor HTTP client
	private val httpClient = HttpClient( Android ) {

		// Set timeouts
		install( HttpTimeout ) {
			requestTimeoutMillis = 2000
			connectTimeoutMillis = 2000
		}

		// Set user-agent
		install( UserAgent ) {
			// TODO: Only add versions if user opts into anonymous statistics
			agent = "viral32111's location sharing app/${BuildConfig.VERSION_NAME} (Android/${Build.VERSION.RELEASE}; SDK/${Build.VERSION.SDK_INT}) (https://viral32111.com; contact@viral32111.com)"
		}

		// Add JSON support
		install( ContentNegotiation ) { gson() }

	}

	// Runs when the activity is created
	override fun onCreate( savedInstanceState: Bundle? ) {
		super.onCreate( savedInstanceState )
		setContentView( R.layout.activity_server )

		// Get relevant UI controls
		val serverUrlInput = findViewById<EditText>( R.id.serverUrlEditText )
		val statusText = findViewById<TextView>( R.id.serverStatusTextView )
		val switchButton = findViewById<Button>( R.id.serverSwitchButton )
		val backButton = findViewById<Button>( R.id.serverBackButton )
		val loadingSpinner = findViewById<ProgressBar>( R.id.serverLoadingProgressBar )

		// Setup offline preferences - TODO: Move this to shared class
		val offlinePreferences = getSharedPreferences( getString( R.string.config_sharedpreferences_offline ), Context.MODE_PRIVATE )
		with ( offlinePreferences.edit() ) {

			// Set the default server URL
			if ( !offlinePreferences.contains( getString( R.string.config_sharedpreferences_offline_serverurl ) ) ) {
				putString( getString( R.string.config_sharedpreferences_offline_serverurl ), defaultServerUrl )
				Log.d( logTag, "Initialised offline preference for server URL as: '${defaultServerUrl}'" )
			}

			// Save any changes
			apply()

		}

		// Only enable the switch button if a URL has been entered, and it is not the same as the current URL
		serverUrlInput.addTextChangedListener {
			val currentServerUrl = offlinePreferences.getString( getString( R.string.config_sharedpreferences_offline_serverurl ), defaultServerUrl )!!
			val newServerUrl = serverUrlInput.text.toString()

			switchButton.isEnabled = newServerUrl.isNotBlank() && currentServerUrl != newServerUrl
			Log.d( logTag, "Enable switch button for URL '${newServerUrl}': ${switchButton.isEnabled} (${newServerUrl.isNotBlank()} && ${currentServerUrl != newServerUrl})" )
		}

		// Runs when the switch button is pressed
		switchButton.setOnClickListener {
			var serverUrl = serverUrlInput.text.toString()

			// Prepend HTTPS scheme to the URL if it has no scheme
			if ( !serverUrl.startsWith( "http://" ) && !serverUrl.startsWith( "https://" ) ) {
				serverUrl = "https://$serverUrl"
				Log.d( logTag, "Added HTTPs scheme to server URL: '${serverUrl}'" )
			}

			// Remove trailing slashes
			serverUrl = serverUrl.trimEnd( '/' )

			// Reset the status UI
			statusText.text = ""
			statusText.setTextColor( ContextCompat.getColor( applicationContext, R.color.failure ) )

			// Disable the URL input
			serverUrlInput.isEnabled = false

			// Show the loading spinner
			loadingSpinner.visibility = View.VISIBLE

			// Run safely inside suspendable block
			lifecycleScope.launch {
				try {

					// Fetch the status of the new server
					Log.i( logTag, "Fetching status from server URL: '${serverUrl}'..." )
					val statusResponse = httpClient.get( "${serverUrl}/status" )
					Log.i( logTag, "Received HTTP status code '${statusResponse.status}' from status check" )

					// Fail if the request was unsuccessful
					if ( statusResponse.status != HttpStatusCode.OK ) throw Exception( "Received unsuccessful HTTP status code: ${statusResponse.status}" )

					// Parse the response as JSON
					val statusPayload: JsonObject = statusResponse.body()

					// Ensure the version key exists
					if ( !statusPayload.has( "version" ) ) throw Exception( "Status payload does not contain version key" )

					// Get the version string
					val serverVersion = statusPayload.get( "version" ).asString

					// Update the status UI
					statusText.text = getString( R.string.server_text_status_success ).format( serverVersion )
					statusText.setTextColor( ContextCompat.getColor( applicationContext, R.color.success ) )

					// Save the new server URL in offline preferences
					with ( offlinePreferences.edit() ) {
						putString( getString( R.string.config_sharedpreferences_offline_serverurl ), serverUrl )
						apply()
					}

				// Update status UI with any errors that occur
				} catch ( exception: Exception ) {
					statusText.text = getString( R.string.server_text_status_failure ).format( exception.message )
				}

				// Enable the URL input
				serverUrlInput.isEnabled = true

				// Hide the loading spinner
				loadingSpinner.visibility = View.GONE
			}
		}

		// Play slide out animation when the back button is pressed (NOT the Android back action!)
		backButton.setOnClickListener {
			finish()
			overridePendingTransition( R.anim.slide_in_from_left, R.anim.slide_out_to_right )
		}

		// Update the URL input with the current server URL stored in offline preferences
		val serverUrl = offlinePreferences.getString( getString( R.string.config_sharedpreferences_offline_serverurl ), defaultServerUrl )!!
		serverUrlInput.setText( serverUrl )
		Log.d( logTag, "Retrieved server URL: '${serverUrl}' from offline preferences" )
	}

	// Play slide out animation when the back action occurs (NOT the custom back button!)
	override fun onBackPressed() {
		super.onBackPressed()
		overridePendingTransition( R.anim.slide_in_from_left, R.anim.slide_out_to_right )
	}

	// Close the HTTP client when the activity finishes
	override fun onDestroy() {
		super.onDestroy()
		httpClient.close()
	}

}
