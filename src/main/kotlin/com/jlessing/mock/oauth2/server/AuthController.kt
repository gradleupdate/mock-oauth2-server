package com.jlessing.mock.oauth2.server

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
import org.springframework.web.client.HttpServerErrorException
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

        val code = (1..40).map { Random.nextInt(0, 62) }
                .map((('a'..'z') + ('A'..'Z') + ('0'..'9'))::get)
                .joinToString("")
        appRegistration.validCodes[code] = this.state.activeUser

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("$redirect_uri?code=$code&state=$state")).build()
    }

    @PostMapping("/oauth/access_token")
    fun accessToken(httpServletRequest: HttpServletRequest, grant_type: String?, code: String?, redirect_uri: String?, client_id: String?, client_secret: String?): ResponseEntity<*> {
        if (grant_type == null) throw BadRequesException("Missing parameter grant_type")
        if (code == null) throw BadRequesException("Missing parameter code")
        if (redirect_uri == null) throw BadRequesException("Missing parameter redirect_uri")

        if (grant_type != "code") throw BadRequesException("grant_type must be 'code'")

        val clientIdVal = client_id
                ?: if (httpServletRequest.getHeader("Authorization").trim().toLowerCase().startsWith("basic"))
                    String(Base64.getDecoder().decode(httpServletRequest.getHeader("Authorization").trim().substring(5).trim()), StandardCharsets.UTF_8).let { it.substring(0, it.indexOf(":")) }
                else throw BadRequesException("Missing client_id")

        val clientSecretVal = client_secret
                ?: if (httpServletRequest.getHeader("Authorization").trim().toLowerCase().startsWith("basic"))
                    String(Base64.getDecoder().decode(httpServletRequest.getHeader("Authorization").trim().substring(5).trim()), StandardCharsets.UTF_8).let { it.substring(it.indexOf(":") + 1) }
                else throw BadRequesException("Missing client_secret")

        val appRegistration = this.state.appRegistrations.firstOrNull { it.clientId == clientIdVal }
                ?: throw BadRequesException("Unknown client_id")

        if (appRegistration.clientSecret != clientSecretVal) throw BadRequesException("Invalid client_secret")
        if (!appRegistration.redirectUrls.contains(redirect_uri)) throw BadRequesException("Invalid redirect_uri")
        if (!appRegistration.validCodes.keys.contains(code)) throw BadRequesException("Invalid code")

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(ObjectMapper().writeValueAsString(mapOf(
                        Pair("token_type", "Bearer"),
                        Pair("access_token", createJWT(UUID.randomUUID().toString(), this.state.activeUser.id.toString()))
                )))
    }

    @GetMapping("/me")
    fun me(httpServletRequest: HttpServletRequest): ResponseEntity<*> = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(ObjectMapper().writeValueAsString(
            this.state.users.firstOrNull { it.id.toString() == decodeJWT(httpServletRequest.getHeader("Authorization").substring(6).trim()).subject }
                    ?: throw HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "User with Id<${decodeJWT(httpServletRequest.getHeader("Authorization").substring(5).trim()).subject}> not found")
    ))

    companion object {
        private const val SECRET_KEY = "SecretSigningKey"

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