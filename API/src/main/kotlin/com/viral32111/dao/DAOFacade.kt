package com.viral32111.dao

import com.viral32111.models.User

// https://ktor.io/docs/interactive-website-add-persistence.html#persistence_logic
interface DAOFacade {

	suspend fun allUsers(): List<User>
	suspend fun user( identifier: Int ): User?

	suspend fun addUser( name: String, age: Int ): User?
	suspend fun editUser( identifier: Int, name: String, age: Int ): Boolean
	suspend fun deleteUser( identifier: Int ): Boolean

}
