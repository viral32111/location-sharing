package com.viral32111.plugins

import com.google.gson.JsonObject
import com.viral32111.MySession
import com.viral32111.dao.dao
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

// Default routing
fun Application.setupRouting() {
	log.info( "Setting up default routing." )

	routing {

		// Status
		get( "/status" ) {
			val status = JsonObject()
			status.addProperty( "version", "0.0.1" )


			call.respond( status )
		}

		// Hello World
		get( "/hello" ) {
			call.respondText( "Hello World!", ContentType.Text.Plain )
		}

		// Redirect root to Hello World
		get( "/" ) {
			call.respondRedirect( "/hello", permanent = false )
		}

		// Get all users - https://ktor.io/docs/interactive-website-add-persistence.html#update_routes
		get( "/users" ) {
			call.respond( mapOf( "users" to dao.allUsers() ) )
		}

		// Useful for testing in browsers
		get( "/favicon.ico" ) {
			call.respond( HttpStatusCode.Gone )
		}

		// Create session
		get( "/session/create" ) {
			call.sessions.set( MySession(
				identifier = 1,
				data = "I'm example data!"
			) )

			call.respond( HttpStatusCode.OK )
		}

		// View session
		get( "/session/view" ) {
			val session = call.sessions.get<MySession>()
			if ( session != null ) {
				call.respondText( session.data, ContentType.Text.Plain )
			} else {
				call.respond( HttpStatusCode.BadRequest )
			}
		}

		// Delete session
		get( "/session/delete" ) {
			call.sessions.clear<MySession>()
			call.respond( HttpStatusCode.OK )
		}

	}
}
