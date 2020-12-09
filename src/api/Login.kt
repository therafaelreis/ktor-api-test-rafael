package com.therafaelreis.api

import com.therafaelreis.JWTService
import com.therafaelreis.hash
import com.therafaelreis.redirect
import com.therafaelreis.repository.Repository
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

const val LOGIN_ENDPOINT = "/login"

@Location(LOGIN_ENDPOINT)
class Login

fun Route.login(repository: Repository, jwtService: JWTService){
    post<Login> {
        val params = call.receive<Parameters>()
        val userId = params["userId"] ?: return@post call.redirect(it)
        val password = params["password"] ?: return@post call.redirect(it)

        val user = repository.user(userId, hash(password))

        if(user != null){
            val token = jwtService.generateToken(user)
            call.respondText(token)
        }else{
            call.respondText("Invalid user")
        }
    }
}