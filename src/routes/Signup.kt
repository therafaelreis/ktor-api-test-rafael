package com.therafaelreis.routes

import com.therafaelreis.MIN_PASSWORD_LENGTH
import com.therafaelreis.MIN_USER_ID_LENGTH
import com.therafaelreis.model.EPSession
import com.therafaelreis.model.User
import com.therafaelreis.redirect
import com.therafaelreis.repository.Repository
import com.therafaelreis.userNameValid
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*

const val SIGN_UP = "/signup"

@Location(SIGN_UP)
data class Signup(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val error: String = ""
)

fun Route.signup(repository: Repository, hash: (String) -> String) {
    post<Signup> {
        val user = call.sessions.get<EPSession>()?.let { repository.user(it.userId) }

        if (user != null) return@post call.redirect(Phrases())

        val signupParameters = call.receive<Parameters>()
        val userId = signupParameters["userId"] ?: return@post call.redirect(it)
        val password = signupParameters["password"] ?: return@post call.redirect(it)
        val displayName = signupParameters["displayName"] ?: return@post call.redirect(it)
        val email = signupParameters["email"] ?: return@post call.redirect(it)

        val signUpError = Signup(userId, displayName, email)
        when {
            password.length < MIN_PASSWORD_LENGTH -> {
                call.redirect(signUpError.copy(error = "Password should be at least $MIN_PASSWORD_LENGTH characters long"))
            }
            userId.length < MIN_USER_ID_LENGTH -> {
                call.redirect(signUpError.copy(error = "Username should be at least $MIN_USER_ID_LENGTH characters long"))
            }
            !userNameValid(userId) -> {
                call.redirect(signUpError.copy(error = "Username should consist of digits, letters, dots or underscores"))
            }
            repository.user(userId) != null -> {
                call.redirect(signUpError.copy(error = "User with the following username is already registered"))
            }
            else -> {
                val hash = hash(password)
                val newUser = User(userId, email, displayName, hash)

                try {
                    repository.createUser(newUser)
                } catch (e: Throwable) {
                    when {
                        repository.user(userId) != null -> {
                            call.redirect(signUpError.copy(error = "User with the following username is already registered."))
                        }
                        repository.userByEmail(email) != null -> {
                            call.redirect(signUpError.copy(error = "User with the following email $email is already registered."))
                        }
                        else -> {
                            application.log.error("Failed to register user", e)
                            call.redirect(signUpError.copy(error = "Failed to register"))
                        }
                    }
                }

                call.sessions.set(EPSession(newUser.userId))
                call.redirect(Phrases())
            }
        }
    }

    get<Signup> {
        val user = call.sessions.get<EPSession>()?.let{ repository.user(it.userId)}
        if(user != null){
            call.redirect(Phrases())
        }else {
            call.respond(FreeMarkerContent("signup.ftl", mapOf("error" to it.error)))
        }
    }
}