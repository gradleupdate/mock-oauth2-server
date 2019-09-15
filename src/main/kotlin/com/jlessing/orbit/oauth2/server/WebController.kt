package com.jlessing.orbit.oauth2.server

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.xml.bind.DatatypeConverter
import kotlin.random.Random


@RestController
class WebController(private val state: State) {

    @GetMapping("/dialog/oauth")
    @Valid
    fun dialog(@NotEmpty response_type: String, @NotEmpty client_id: String, @NotEmpty scope: String, @NotEmpty state: String, @NotEmpty redirect_uri: String): ResponseEntity<Unit> {
        if (response_type != "code") throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "response_type must be 'code'")

        val appRegistration = this.state.appRegistrations.firstOrNull { it.clientId == client_id }
                ?: throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Unknown client_id")
        if (!appRegistration.redirectUrls.contains(redirect_uri.trim())) throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid redirect_uri")

        val scopes = scope.split(" ")
        if (!scopes.contains("user:email")) throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing scope 'user:email'")
        if (!scopes.contains("read:user")) throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing scope 'read:user'")

        val code = (1..40).map { Random.nextInt(0, 62) }
                .map((('a'..'z') + ('A'..'Z') + ('0'..'9'))::get)
                .joinToString("")
        appRegistration.validCodes[code] = this.state.activeUser

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("$redirect_uri?code=$code&state=$state")).build()
    }

    @PostMapping("/oauth/access_token")
    @Valid
    fun accessToken(httpServletRequest: HttpServletRequest, @NotEmpty grant_type: String, @NotEmpty code: String, @NotEmpty redirect_uri: String, client_id: String?, client_secret: String?): ResponseEntity<*> {
        if (grant_type != "code") throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "grant_type must be 'code'")

        val clientIdVal = client_id
                ?: if (httpServletRequest.authType == HttpServletRequest.BASIC_AUTH)
                    String(Base64.getDecoder().decode(httpServletRequest.getHeader("Authorization").trim().substring(5).trim()), StandardCharsets.UTF_8).let { it.substring(0, it.indexOf(":")) }
                else throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing client_id")

        val clientSecretVal = client_secret
                ?: if (httpServletRequest.authType == HttpServletRequest.BASIC_AUTH)
                    String(Base64.getDecoder().decode(httpServletRequest.getHeader("Authorization").trim().substring(5).trim()), StandardCharsets.UTF_8).let { it.substring(it.indexOf(":") + 1) }
                else throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing client_secret")

        val appRegistration = this.state.appRegistrations.firstOrNull { it.clientId == clientIdVal }
                ?: throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Unknown client_id")

        if (appRegistration.clientSecret != clientSecretVal) throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid client_secret")
        if (!appRegistration.redirectUrls.contains(redirect_uri)) throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid redirect_uri")
        if (!appRegistration.validCodes.keys.contains(code)) throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid code")

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(ObjectMapper().writeValueAsString(mapOf(
                        Pair("token_type", "Bearer"),
                        Pair("access_token", createJWT(UUID.randomUUID().toString(), "Me", this.state.activeUser.id.toString()))
                )))
    }

    @GetMapping("/me")
    fun me(httpServletRequest: HttpServletRequest): ResponseEntity<*> = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(ObjectMapper().writeValueAsString(
            this.state.users.firstOrNull { it.id.toString() == decodeJWT(httpServletRequest.getHeader("Authorization").substring(5).trim()).subject }
                    ?: throw HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "User with Id<${decodeJWT(httpServletRequest.getHeader("Authorization").substring(5).trim()).subject}> not found")
    ))


    @RequestMapping("/**")
    fun test(httpServletRequest: HttpServletRequest) {
        println()
        println(httpServletRequest.method)
        println(httpServletRequest.requestURI)
        println(httpServletRequest.parameterMap.entries.map { it.key.toString() + ": " + it.value.joinToString(", ") }.joinToString("\n"))
        println(httpServletRequest.inputStream.bufferedReader().use { it.readText() })
        println()
    }

    companion object {
        private val SECRET_KEY = "SecretSigningKey"

        private fun createJWT(id: String, issuer: String, subject: String) = Jwts.builder()
                .setId(id)
                .setIssuedAt(Date(System.currentTimeMillis()))
                .setSubject(subject)
                .setIssuer(issuer)
                .signWith(
                        SignatureAlgorithm.HS256,
                        SecretKeySpec(
                                DatatypeConverter.parseBase64Binary(SECRET_KEY),
                                SignatureAlgorithm.HS256.jcaName
                        )
                ).compact()

        private fun decodeJWT(jwt: String) = Jwts.parser()
                .setSigningKey(DatatypeConverter.parseBase64Binary(SECRET_KEY))
                .parseClaimsJws(jwt).body

    }

}