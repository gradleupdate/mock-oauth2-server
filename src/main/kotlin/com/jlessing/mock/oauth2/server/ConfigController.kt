package com.jlessing.mock.oauth2.server

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import org.springframework.hateoas.Resource
import org.springframework.hateoas.Resources
import org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo
import org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/config")
class ConfigController(private val state: State) {
    @PostMapping("/users")
    fun createUser(@RequestBody createUserRequest: CreateUserRequest): Resource<User> =
            Resource(User(createUserRequest.name, createUserRequest.email).apply { state.users.add(this) })

    @GetMapping("/user/{id}")
    fun getUser(@PathVariable id: Int): Resource<User> = Resource(state.users.find { it.id == id }!!).apply { this.add(linkTo(methodOn(ConfigController::class.java).getUser(id)).withSelfRel()) }

    @GetMapping("/users")
    fun getUsers(): Resources<Resource<User>> = Resources(state.users.map { Resource(it).apply { this.add(linkTo(methodOn(ConfigController::class.java).getUser(it.id)).withSelfRel()) } }, linkTo(methodOn(ConfigController::class.java).getUsers()).withSelfRel())

    @GetMapping("/activeUser")
    fun getActiveUser(): Resource<User> = Resource(state.activeUser).apply { this.add(linkTo(methodOn(ConfigController::class.java).getUser(state.activeUser.id)).withSelfRel()) }

    @PutMapping("/activeUser")
    fun setActiveUser(@RequestBody setActiveUserRequest: SetActiveUserRequest) {
        state.activeUser = state.users.firstOrNull { it.id == setActiveUserRequest.id }
                ?: throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "User with Id<${setActiveUserRequest.id}> not found")
    }

    @GetMapping("/appRegistrations")
    fun getAppRegistrations(): Resources<Resource<AppRegistration>> = Resources(state.appRegistrations.map { Resource(it).apply { this.add(linkTo(methodOn(ConfigController::class.java).getAppRegistration(it.clientId)).withSelfRel()) } }, linkTo(methodOn(ConfigController::class.java).getAppRegistrations()).withSelfRel())

    @GetMapping("/appRegistrations/{clientId}")
    fun getAppRegistration(@PathVariable clientId: String): Resource<AppRegistration> = Resource(state.appRegistrations.find { it.clientId == clientId }!!).apply { this.add(linkTo(methodOn(ConfigController::class.java).getAppRegistration(clientId)).withSelfRel()) }

    @PostMapping("/appRegistrations")
    fun createAppRegistration(@RequestBody createAppRegistrationRequest: CreateAppRegistrationRequest) =
            AppRegistration(
                    createAppRegistrationRequest.clientId,
                    createAppRegistrationRequest.clientSecret,
                    createAppRegistrationRequest.redirectUrls)
                    .apply { if (state.appRegistrations.any { it.clientId == createAppRegistrationRequest.clientId }) throw BadRequesException("AppRegistration with clientId<${createAppRegistrationRequest.clientId}> already exists") }
                    .apply { state.appRegistrations.add(this) }

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