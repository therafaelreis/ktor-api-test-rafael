package com.therafaelreis.routes

import com.therafaelreis.MIN_PASSWORD_LENGTH
import com.therafaelreis.MIN_USER_ID_LENGTH
import com.therafaelreis.model.EPSession
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

const val SIGNIN = "/signin"

@Location(SIGNIN)
data class Signin(val userId: String = "", val error: String = "")

fun Route.signin(repository: Repository, hash: (String) -> String) {

    post<Signin> {
        val signInParameters = call.receive<Parameters>()
        val userId = signInParameters["userId"] ?: return@post call.redirect(it)
        val password = signInParameters["password"] ?: return@post call.redirect(it)

        val signInError = Signin(userId)

        val signin = when {
            userId.length < MIN_USER_ID_LENGTH -> null
            password.length < MIN_PASSWORD_LENGTH -> null
            !userNameValid(userId) -> null
            else -> repository.user(userId, hash(password))
        }

        if (signin == null) {
            call.redirect(signInError.copy(error = "Invalid username or password"))
        } else {
            call.sessions.set(EPSession(signin.userId))
            call.redirect(Phrases())
        }
    }

    get<Signin> {
        val user = call.sessions.get<EPSession>()?.let { repository.user(it.userId) }

        if(user != null){
            call.redirect(Index())
        }else {
            call.respond(FreeMarkerContent("signin.ftl", mapOf("error" to it.userId, "error" to it.error), ""))
        }
    }
}