package com.therafaelreis.routes

import com.therafaelreis.model.EPSession
import com.therafaelreis.repository.Repository
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*

const val ABOUT = "/about"

@Location(ABOUT)
class About
fun Route.about(repository: Repository){
    get<About>{
        val user = call.sessions.get<EPSession>()?.let{ repository.user(it.userId)}
        call.respond(FreeMarkerContent("about.ftl", mapOf("user" to user)))
    }
}