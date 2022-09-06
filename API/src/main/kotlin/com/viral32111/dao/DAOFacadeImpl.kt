package com.viral32111.dao

import com.viral32111.models.User
import com.viral32111.dao.DatabaseFactory.databaseQuery
import com.viral32111.models.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*

// https://ktor.io/docs/interactive-website-add-persistence.html#persistence_logic
class DAOFacadeImpl : DAOFacade {

	private fun resultRowToUser( row: ResultRow ) = User(
		identifier = row[ Users.identifier ],
		name = row[ Users.name ],
		age = row[ Users.age ]
	)

	override suspend fun allUsers(): List<User> = databaseQuery {
		Users.selectAll().map( ::resultRowToUser )
	}

	override suspend fun user( identifier: Int ): User? = databaseQuery {
		Users.select { Users.identifier eq identifier }.map( ::resultRowToUser ).singleOrNull()
	}

	override suspend fun addUser( name: String, age: Int ): User? = databaseQuery {
		val insertStatement = Users.insert {
			it[ Users.name ] = name
			it[ Users.age ] = age
		}

		insertStatement.resultedValues?.singleOrNull()?.let( ::resultRowToUser )
	}

	override suspend fun editUser( identifier: Int, name: String, age: Int ): Boolean = databaseQuery {
		Users.update( { Users.identifier eq identifier } ) {
			it[ Users.name ] = name
			it[ Users.age ] = age
		} > 0
	}

	override suspend fun deleteUser( identifier: Int ): Boolean = databaseQuery {
		Users.deleteWhere { Users.identifier eq identifier } > 0
	}

}

// https://ktor.io/docs/interactive-website-add-persistence.html#init-dao-facade
val dao: DAOFacade = DAOFacadeImpl().apply {
	runBlocking {
		if ( allUsers().isEmpty() ) {
			addUser( "JohnDoe", 25 )
		}
	}
}
