package com.jlessing.orbit.oauth2.server

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/config")
class ConfigController(private val state: State) {
    @PostMapping("/users")
    fun createUser(@RequestBody createUserRequest: CreateUserRequest): User =
            User(createUserRequest.name, createUserRequest.email).apply { state.users.add(this) }

    @PutMapping("/activeUser")
    fun setActiveUser(@RequestBody setActiveUserRequest: SetActiveUserRequest) {
        state.activeUser = state.users.firstOrNull { it.id == setActiveUserRequest.id }
                ?: throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "User with Id<${setActiveUserRequest.id}> not found")
    }

    @PostMapping("/appRegistrations")
    fun createAppRegistration(@RequestBody createAppRegistrationRequest: CreateAppRegistrationRequest) =
            AppRegistration(
                    createAppRegistrationRequest.clientId,
                    createAppRegistrationRequest.clientSecret,
                    createAppRegistrationRequest.redirectUrls
            ).apply { state.appRegistrations.add(this) }

    @DeleteMapping
    fun reset() {
        state.activeUser = User.DEFAULT_USER
        state.users.removeAll { it != User.DEFAULT_USER }
        state.appRegistrations.clear()
    }
}

data class CreateUserRequest(var name: String, var email: String)

data class SetActiveUserRequest(var id: Int)

data class CreateAppRegistrationRequest(var clientId: String, var clientSecret: String, var redirectUrls: MutableList<String>)