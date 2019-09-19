package com.jlessing.mock.oauth2.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.HttpClientErrorException

@SpringBootApplication
class Application

fun main(args: Array<String>) {
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

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequesException(msg: String) : HttpClientErrorException(HttpStatus.BAD_REQUEST, msg)