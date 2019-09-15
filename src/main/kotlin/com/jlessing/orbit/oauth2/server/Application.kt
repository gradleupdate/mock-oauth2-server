package com.jlessing.orbit.oauth2.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    println(String(Base64.getDecoder().decode("Basic VXNlcjpQYXN3b3Jk".trim().substring(5).trim()), StandardCharsets.UTF_8).let { it.substring(0, it.indexOf(":")) })
    println(String(Base64.getDecoder().decode("Basic VXNlcjpQYXN3b3Jk".trim().substring(5).trim()), StandardCharsets.UTF_8).let { it.substring(it.indexOf(":") + 1) })
    System.exit(2)
    runApplication<Application>(*args)
}

data class User(
        val name: String,
        val email: String
) {
    val id: Int

    init {
        this.id = idCounter++
    }

    companion object {
        private var idCounter: Int = 0
        val DEFAULT_USER = User("MockOAuth2ServerDefaultUser", "MockOAuth2ServerDefaultUser@email.de")
    }
}

data class AppRegistration(
        val clientId: String,
        val clientSecret: String,
        val redirectUrls: MutableList<String> = arrayListOf(),
        val validCodes: MutableMap<String, User> = mutableMapOf()
)

@Component
data class State(
        var activeUser: User = User.DEFAULT_USER,
        val users: MutableList<User> = arrayListOf(User.DEFAULT_USER),
        val appRegistrations: MutableList<AppRegistration> = arrayListOf()
)