package com.jlessing.mock.oauth2.server

import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.HttpClientErrorException
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableSwagger2
class Application {
    @Bean
    fun api(appContext: ConfigurableApplicationContext): Docket = Docket(DocumentationType.SWAGGER_2)
            .apiInfo(ApiInfo(
                    "Mock Oauth2 Server API",
                    "This is the REST-API Documentation of the Mock OAuth2 Server",
                    try {
                        appContext.getBean("version") as String
                    } catch (e: NoSuchBeanDefinitionException) {
                        "latest"
                    },
                    "",
                    Contact("Jonathan Lessing", "www.github.com/jlessing-git/mock-oauth2-server", ""),
                    "GNU GPLv3", "https://github.com/jlessing-git/mock-oauth2-server/blob/master/LICENSE.txt", emptyList()
            ))
            .select().apis(RequestHandlerSelectors.any()).paths(PathSelectors.any()).build()
}

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