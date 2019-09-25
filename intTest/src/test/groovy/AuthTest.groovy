import groovy.json.JsonSlurper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import spock.lang.Specification

import java.nio.charset.StandardCharsets

import static groovy.json.JsonOutput.toJson

class AuthTest extends Specification {

    def authFlow() {
        given:
        reset()

        def createAppRegsitrationConnection = (HttpURLConnection) new URL("http://localhost:8023/config/appRegistrations").openConnection()
        createAppRegsitrationConnection.setDoOutput(true)
        createAppRegsitrationConnection.setRequestMethod("POST")
        createAppRegsitrationConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        createAppRegsitrationConnection.outputStream.write(toJson([
                clientId    : "testClientId",
                clientSecret: "testClientSecret",
                redirectUrls: ["http://localhost:1234/redirect"]
        ]).getBytes())
        createAppRegsitrationConnection.responseCode
        when:
        //Get Authorization Code
        def dialogConnection = (HttpURLConnection) new URL("http://localhost:8023/dialog/oauth?response_type=code&client_id=testClientId&scope=user%3Aemail%20read%3Auser&state=testState&redirect_uri=http%3A%2F%2Flocalhost%3A1234%2Fredirect").openConnection()
        dialogConnection.setDoOutput(true)
        dialogConnection.setInstanceFollowRedirects(false)
        assert dialogConnection.responseCode == 302
        assert dialogConnection.getHeaderField("Location").matches("http://localhost:1234/redirect\\?code=.*?&state=testState")
        def code = dialogConnection.getHeaderField("Location")
        code = code.substring(code.indexOf("?code=") + 6, code.lastIndexOf("&state="))
        assert isAlphanumeric(code)

        //Get Access Token
        def accessTokenConnection = (HttpURLConnection) new URL("http://localhost:8023/oauth/access_token?grant_type=code&code=$code&redirect_uri=http%3A%2F%2Flocalhost%3A1234%2Fredirect&client_id=testClientId&client_secret=testClientSecret").openConnection()
        accessTokenConnection.setDoOutput(true)
        accessTokenConnection.setRequestMethod("POST")
        def accessTokenConnectionBody = new JsonSlurper().parse(accessTokenConnection.inputStream)
        assert accessTokenConnectionBody["token_type"] == "Bearer"
        def claims = decodeJWT((String) accessTokenConnectionBody["access_token"])
        UUID.fromString(claims.getId())
        assert claims.getSubject() == "0"
        assert claims.getIssuer() == "Me"
        assert !claims.getIssuedAt().after(new Date())

        //Get Me
        def meConnection = (HttpURLConnection) new URL("http://localhost:8023/me").openConnection()
        meConnection.setRequestProperty("Authorization", "Bearer ${accessTokenConnectionBody["access_token"]}")
        then:
        def me = new JsonSlurper().parse(meConnection.inputStream)
        assert me.id == 0
        assert me.name == "MockOAuth2ServerDefaultUser"
        assert me.email == "MockOAuth2ServerDefaultUser@email.de"
    }

    def authFlowWithIdAndSecretAsHeader() {
        given:
        reset()

        def createAppRegsitrationConnection = (HttpURLConnection) new URL("http://localhost:8023/config/appRegistrations").openConnection()
        createAppRegsitrationConnection.setDoOutput(true)
        createAppRegsitrationConnection.setRequestMethod("POST")
        createAppRegsitrationConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        createAppRegsitrationConnection.outputStream.write(toJson([
                clientId    : "testClientId",
                clientSecret: "testClientSecret",
                redirectUrls: ["http://localhost:1234/redirect"]
        ]).getBytes())
        createAppRegsitrationConnection.responseCode
        when:
        //Get Authorization Code
        def dialogConnection = (HttpURLConnection) new URL("http://localhost:8023/dialog/oauth?response_type=code&client_id=testClientId&scope=user%3Aemail%20read%3Auser&state=testState&redirect_uri=http%3A%2F%2Flocalhost%3A1234%2Fredirect").openConnection()
        dialogConnection.setDoOutput(true)
        dialogConnection.setInstanceFollowRedirects(false)
        assert dialogConnection.responseCode == 302
        assert dialogConnection.getHeaderField("Location").matches("http://localhost:1234/redirect\\?code=.*?&state=testState")
        def code = dialogConnection.getHeaderField("Location")
        code = code.substring(code.indexOf("?code=") + 6, code.lastIndexOf("&state="))
        assert isAlphanumeric(code)

        //Get Access Token
        def accessTokenConnection = (HttpURLConnection) new URL("http://localhost:8023/oauth/access_token?grant_type=code&code=$code&redirect_uri=http%3A%2F%2Flocalhost%3A1234%2Fredirect").openConnection()
        accessTokenConnection.setDoOutput(true)
        accessTokenConnection.setRequestMethod("POST")
        accessTokenConnection.setRequestProperty("Authorization", "Basic " + new String(Base64.encoder.encode("testClientId:testClientSecret".getBytes())))
        def accessTokenConnectionBody = new JsonSlurper().parse(accessTokenConnection.inputStream)
        assert accessTokenConnectionBody["token_type"] == "Bearer"
        def claims = decodeJWT((String) accessTokenConnectionBody["access_token"])
        UUID.fromString(claims.getId())
        assert claims.getSubject() == "0"
        assert claims.getIssuer() == "Me"
        assert !claims.getIssuedAt().after(new Date())

        //Get Me
        def meConnection = (HttpURLConnection) new URL("http://localhost:8023/me").openConnection()
        meConnection.setRequestProperty("Authorization", "Bearer ${accessTokenConnectionBody["access_token"]}")
        then:
        def me = new JsonSlurper().parse(meConnection.inputStream)
        assert me.id == 0
        assert me.name == "MockOAuth2ServerDefaultUser"
        assert me.email == "MockOAuth2ServerDefaultUser@email.de"
    }

    def authFlowAccessTokenRequestAsBody() {
        given:
        reset()

        def createAppRegsitrationConnection = (HttpURLConnection) new URL("http://localhost:8023/config/appRegistrations").openConnection()
        createAppRegsitrationConnection.setDoOutput(true)
        createAppRegsitrationConnection.setRequestMethod("POST")
        createAppRegsitrationConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        createAppRegsitrationConnection.outputStream.write(toJson([
                clientId    : "testClientId",
                clientSecret: "testClientSecret",
                redirectUrls: ["http://localhost:1234/redirect"]
        ]).getBytes())
        createAppRegsitrationConnection.responseCode
        when:
        //Get Authorization Code
        def dialogConnection = (HttpURLConnection) new URL("http://localhost:8023/dialog/oauth?response_type=code&client_id=testClientId&scope=user%3Aemail%20read%3Auser&state=testState&redirect_uri=http%3A%2F%2Flocalhost%3A1234%2Fredirect").openConnection()
        dialogConnection.setDoOutput(true)
        dialogConnection.setInstanceFollowRedirects(false)
        assert dialogConnection.responseCode == 302
        assert dialogConnection.getHeaderField("Location").matches("http://localhost:1234/redirect\\?code=.*?&state=testState")
        def code = dialogConnection.getHeaderField("Location")
        code = code.substring(code.indexOf("?code=") + 6, code.lastIndexOf("&state="))
        assert isAlphanumeric(code)

        //Get Access Token
        def accessTokenConnection = (HttpURLConnection) new URL("http://localhost:8023/oauth/access_token").openConnection()
        accessTokenConnection.setDoOutput(true)
        accessTokenConnection.setRequestMethod("POST")
        accessTokenConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        accessTokenConnection.outputStream.write(toJson([
                grant_type   : "code",
                code         : code,
                redirect_uri : "http://localhost:1234/redirect",
                client_id    : "testClientId",
                client_secret: "testClientSecret"
        ]).getBytes())
        def accessTokenConnectionBody = new JsonSlurper().parse(accessTokenConnection.inputStream)
        assert accessTokenConnectionBody["token_type"] == "Bearer"
        def claims = decodeJWT((String) accessTokenConnectionBody["access_token"])
        UUID.fromString(claims.getId())
        assert claims.getSubject() == "0"
        assert claims.getIssuer() == "Me"
        assert !claims.getIssuedAt().after(new Date())

        //Get Me
        def meConnection = (HttpURLConnection) new URL("http://localhost:8023/me").openConnection()
        meConnection.setRequestProperty("Authorization", "Bearer ${accessTokenConnectionBody["access_token"]}")
        then:
        def me = new JsonSlurper().parse(meConnection.inputStream)
        assert me.id == 0
        assert me.name == "MockOAuth2ServerDefaultUser"
        assert me.email == "MockOAuth2ServerDefaultUser@email.de"
    }

    def authFlowAccessTokenRequestAsBodyAndAuthAsHeader() {
        given:
        reset()

        def createAppRegsitrationConnection = (HttpURLConnection) new URL("http://localhost:8023/config/appRegistrations").openConnection()
        createAppRegsitrationConnection.setDoOutput(true)
        createAppRegsitrationConnection.setRequestMethod("POST")
        createAppRegsitrationConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        createAppRegsitrationConnection.outputStream.write(toJson([
                clientId    : "testClientId",
                clientSecret: "testClientSecret",
                redirectUrls: ["http://localhost:1234/redirect"]
        ]).getBytes())
        createAppRegsitrationConnection.responseCode
        when:
        //Get Authorization Code
        def dialogConnection = (HttpURLConnection) new URL("http://localhost:8023/dialog/oauth?response_type=code&client_id=testClientId&scope=user%3Aemail%20read%3Auser&state=testState&redirect_uri=http%3A%2F%2Flocalhost%3A1234%2Fredirect").openConnection()
        dialogConnection.setDoOutput(true)
        dialogConnection.setInstanceFollowRedirects(false)
        assert dialogConnection.responseCode == 302
        assert dialogConnection.getHeaderField("Location").matches("http://localhost:1234/redirect\\?code=.*?&state=testState")
        def code = dialogConnection.getHeaderField("Location")
        code = code.substring(code.indexOf("?code=") + 6, code.lastIndexOf("&state="))
        assert isAlphanumeric(code)

        //Get Access Token
        def accessTokenConnection = (HttpURLConnection) new URL("http://localhost:8023/oauth/access_token").openConnection()
        accessTokenConnection.setDoOutput(true)
        accessTokenConnection.setRequestMethod("POST")
        accessTokenConnection.setRequestProperty("Authorization", "Basic " + new String(Base64.encoder.encode("testClientId:testClientSecret".getBytes())))
        accessTokenConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        accessTokenConnection.outputStream.write(toJson([
                grant_type  : "code",
                code        : code,
                redirect_uri: "http://localhost:1234/redirect"
        ]).getBytes())
        def accessTokenConnectionBody = new JsonSlurper().parse(accessTokenConnection.inputStream)
        assert accessTokenConnectionBody["token_type"] == "Bearer"
        def claims = decodeJWT((String) accessTokenConnectionBody["access_token"])
        UUID.fromString(claims.getId())
        assert claims.getSubject() == "0"
        assert claims.getIssuer() == "Me"
        assert !claims.getIssuedAt().after(new Date())

        //Get Me
        def meConnection = (HttpURLConnection) new URL("http://localhost:8023/me").openConnection()
        meConnection.setRequestProperty("Authorization", "Bearer ${accessTokenConnectionBody["access_token"]}")
        then:
        def me = new JsonSlurper().parse(meConnection.inputStream)
        assert me.id == 0
        assert me.name == "MockOAuth2ServerDefaultUser"
        assert me.email == "MockOAuth2ServerDefaultUser@email.de"
    }

    def authFlowWithCustomUser() {
        given:
        reset()

        def createAppRegsitrationConnection = (HttpURLConnection) new URL("http://localhost:8023/config/appRegistrations").openConnection()
        createAppRegsitrationConnection.setDoOutput(true)
        createAppRegsitrationConnection.setRequestMethod("POST")
        createAppRegsitrationConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        createAppRegsitrationConnection.outputStream.write(toJson([
                clientId    : "testClientId",
                clientSecret: "testClientSecret",
                redirectUrls: ["http://localhost:1234/redirect"]
        ]).getBytes())
        assert createAppRegsitrationConnection.responseCode == 200

        def createUserConnection = (HttpURLConnection) new URL("http://localhost:8023/config/users").openConnection()
        createUserConnection.setDoOutput(true)
        createUserConnection.setRequestMethod("POST")
        createUserConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        createUserConnection.outputStream.write(toJson([
                name : "testName",
                email: "test@email.de"
        ]).getBytes())
        assert createUserConnection.responseCode == 200
        def userId = (String) new JsonSlurper().parse(createUserConnection.inputStream).id

        def setActiveUserConnection = (HttpURLConnection) new URL("http://localhost:8023/config/activeUser").openConnection()
        setActiveUserConnection.setDoOutput(true)
        setActiveUserConnection.setRequestMethod("PUT")
        setActiveUserConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setActiveUserConnection.outputStream.write(toJson([
                id: userId
        ]).getBytes())
        assert setActiveUserConnection.responseCode == 200

        when:
        //Get Authorization Code
        def dialogConnection = (HttpURLConnection) new URL("http://localhost:8023/dialog/oauth?response_type=code&client_id=testClientId&scope=user%3Aemail%20read%3Auser&state=testState&redirect_uri=http%3A%2F%2Flocalhost%3A1234%2Fredirect").openConnection()
        dialogConnection.setDoOutput(true)
        dialogConnection.setInstanceFollowRedirects(false)
        assert dialogConnection.responseCode == 302
        assert dialogConnection.getHeaderField("Location").matches("http://localhost:1234/redirect\\?code=.*?&state=testState")
        def code = dialogConnection.getHeaderField("Location")
        code = code.substring(code.indexOf("?code=") + 6, code.lastIndexOf("&state="))
        assert isAlphanumeric(code)

        //Get Access Token
        def accessTokenConnection = (HttpURLConnection) new URL("http://localhost:8023/oauth/access_token?grant_type=code&code=$code&redirect_uri=http%3A%2F%2Flocalhost%3A1234%2Fredirect&client_id=testClientId&client_secret=testClientSecret").openConnection()
        accessTokenConnection.setDoOutput(true)
        accessTokenConnection.setRequestMethod("POST")
        def accessTokenConnectionBody = new JsonSlurper().parse(accessTokenConnection.inputStream)
        assert accessTokenConnectionBody["token_type"] == "Bearer"
        def claims = decodeJWT((String) accessTokenConnectionBody["access_token"])
        UUID.fromString(claims.getId())
        assert claims.getSubject() == userId
        assert claims.getIssuer() == "Me"
        assert !claims.getIssuedAt().after(new Date())

        //Get Me
        def meConnection = (HttpURLConnection) new URL("http://localhost:8023/me").openConnection()
        meConnection.setRequestProperty("Authorization", "Bearer ${accessTokenConnectionBody["access_token"]}")
        then:
        def me = new JsonSlurper().parse(meConnection.inputStream)
        assert me.id == userId.toInteger()
        assert me.name == "testName"
        assert me.email == "test@email.de"
    }

    static Claims decodeJWT(String jwt) {
        return Jwts.parser()
                .setSigningKey("SecretSigningKey".getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(jwt).body
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    static boolean isAlphanumeric(String s) {
        List<Character> chars = new ArrayList<>()
        for (int i = 0; i < 26; i++)
            chars.add((char) (('a'.charAt(0)) + i))
        for (int i = 0; i < 26; i++)
            chars.add((char) (('A'.charAt(0)) + i))
        for (int i = 0; i < 10; i++)
            chars.add((char) (('0'.charAt(0)) + i))

        for (char c in s.chars)
            if (!chars.contains(c))
                return false
        return true
    }

    static void reset() {
        def resetConnection = (HttpURLConnection) new URL("http://localhost:8023/config").openConnection()
        resetConnection.setRequestMethod("DELETE")
        assert resetConnection.responseCode == 200
    }

}
