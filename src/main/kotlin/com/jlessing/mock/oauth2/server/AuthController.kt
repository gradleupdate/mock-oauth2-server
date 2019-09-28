package com.jlessing.mock.oauth2.server

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest
import kotlin.random.Random

@RestController
class AuthController(private val state: State) {

    @GetMapping("/dialog/oauth")
    fun dialog(response_type: String?, client_id: String?, scope: String?, state: String?, redirect_uri: String?): ResponseEntity<Unit> {
        logger.info("Dialog Endpoint called with Parameters client_id<$client_id>, scope<$scope>, state<$state>, redirect_uri<$redirect_uri>")
        if (response_type == null) throw BadRequesException("Missing parameter response_type")
        if (client_id == null) throw BadRequesException("Missing parameter client_id")
        if (scope == null) throw BadRequesException("Missing parameter scope")
        if (state == null) throw BadRequesException("Missing parameter state")
        if (redirect_uri == null) throw BadRequesException("Missing parameter redirect_uri")

        if (response_type != "code") throw BadRequesException("response_type must be 'code'")

        val appRegistration = this.state.appRegistrations.firstOrNull { it.clientId == client_id }
                ?: throw BadRequesException("Unknown client_id")
        if (!appRegistration.redirectUrls.contains(redirect_uri.trim())) throw BadRequesException("Invalid redirect_uri")

        val scopes = scope.split(" ")
        if (!scopes.contains("user:email")) throw BadRequesException("Missing scope 'user:email'")
        if (!scopes.contains("read:user")) throw BadRequesException("Missing scope 'read:user'")

        logger.info("Creating Grant Code for User<${this.state.activeUser.id}>")
        val code = (1..40).map { Random.nextInt(0, 62) }
                .map((('a'..'z') + ('A'..'Z') + ('0'..'9'))::get)
                .joinToString("")
        appRegistration.validCodes[code] = this.state.activeUser

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("$redirect_uri?code=$code&state=$state")).build()
    }

    @PostMapping("/oauth/access_token")
    fun accessToken(httpServletRequest: HttpServletRequest, grant_type: String?, code: String?, redirect_uri: String?, client_id: String?, client_secret: String?, @RequestBody requestBody: Map<String, String>?): ResponseEntity<*> {
        logger.info("Access Token Endpoint called with Parameters grant_type<$grant_type>, code<$code>, redirect_uri<$redirect_uri>, client_id<$client_id>, client_secret<$client_secret> and Authorization Header<${httpServletRequest.getHeader("Authorization")}> and Request Body<${if (requestBody != null) ObjectMapper().writeValueAsString(requestBody) else null}>")
        val grantTypeVal = grant_type
                ?: requestBody?.get("grant_type")
                ?: throw BadRequesException("Missing parameter grant_type")
        val codeVal = code
                ?: requestBody?.get("code")
                ?: throw BadRequesException("Missing parameter code")
        val redirectUriVal = redirect_uri
                ?: requestBody?.get("redirect_uri")
                ?: throw BadRequesException("Missing parameter redirect_uri")

        if (grantTypeVal != "code") throw BadRequesException("grant_type must be 'code'")

        val clientIdVal = client_id
                ?: requestBody?.get("client_id")
                ?: if (httpServletRequest.getHeader("Authorization").trim().toLowerCase().startsWith("basic"))
                    String(Base64.getDecoder().decode(httpServletRequest.getHeader("Authorization").trim().substring(5).trim()), StandardCharsets.UTF_8).let { it.substring(0, it.indexOf(":")) }
                else throw BadRequesException("Missing client_id")

        val clientSecretVal = client_secret
                ?: requestBody?.get("client_secret")
                ?: if (httpServletRequest.getHeader("Authorization").trim().toLowerCase().startsWith("basic"))
                    String(Base64.getDecoder().decode(httpServletRequest.getHeader("Authorization").trim().substring(5).trim()), StandardCharsets.UTF_8).let { it.substring(it.indexOf(":") + 1) }
                else throw BadRequesException("Missing client_secret")

        val appRegistration = this.state.appRegistrations.firstOrNull { it.clientId == clientIdVal }
                ?: throw BadRequesException("Unknown client_id")

        if (appRegistration.clientSecret != clientSecretVal) throw BadRequesException("Invalid client_secret")
        if (!appRegistration.redirectUrls.contains(redirectUriVal)) throw BadRequesException("Invalid redirect_uri")
        if (!appRegistration.validCodes.keys.contains(codeVal)) throw BadRequesException("Invalid code")
        if ("" in listOf<String>()) {
            println("jo")
        }
        logger.info("Creating JWT for User<${appRegistration.validCodes[codeVal]!!.id}>")
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(ObjectMapper().writeValueAsString(mapOf(
                        Pair("token_type", "Bearer"),
                        Pair("access_token", createJWT(UUID.randomUUID().toString(), appRegistration.validCodes[codeVal]!!.id.toString()))
                )))
    }

    @GetMapping("/me")
    fun me(httpServletRequest: HttpServletRequest): ResponseEntity<*> =
            if ("Authorization" !in httpServletRequest.headerNames.toList())
                throw UnauthorizedException("Authorization header missing")
            else
                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(ObjectMapper().writeValueAsString(
                        this.state.users.firstOrNull { it.id.toString() == decodeJWT(httpServletRequest.getHeader("Authorization").substring(6).trim()).subject }
                                ?: throw BadRequesException("User with Id<${decodeJWT(httpServletRequest.getHeader("Authorization").substring(5).trim()).subject}> not found")
                )).also { logger.info("Me Endpoint Called as User<${decodeJWT(httpServletRequest.getHeader("Authorization").substring(6).trim()).subject}>") }

    companion object {
        private const val SECRET_KEY = "SecretSigningKey"
        private val logger = LoggerFactory.getLogger(AuthController::class.java)

        private fun createJWT(id: String, subject: String) = Jwts.builder()
                .setId(id)
                .setIssuedAt(Date(System.currentTimeMillis()))
                .setSubject(subject)
                .setIssuer("Me")
                .signWith(
                        SignatureAlgorithm.HS256,
                        SecretKeySpec(
                                SECRET_KEY.toByteArray(StandardCharsets.UTF_8),
                                SignatureAlgorithm.HS256.jcaName
                        )
                ).compact()

        private fun decodeJWT(jwt: String) = Jwts.parser()
                .setSigningKey(SECRET_KEY.toByteArray(StandardCharsets.UTF_8))
                .parseClaimsJws(jwt).body

    }

}