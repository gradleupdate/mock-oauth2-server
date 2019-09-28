package com.jlessing.mock.oauth2.server

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@RunWith(SpringRunner::class)
@WebMvcTest(AuthController::class)
class AuthControllerTest {
    @Rule
    @JvmField
    var expectedException: ExpectedException = ExpectedException.none()
    @Autowired
    private var context: WebApplicationContext? = null
    private var mockMvc: MockMvc? = null
    @MockBean
    private var state: State? = null

    @Before
    fun setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context!!).build()

        `when`(state!!.activeUser).thenReturn(User.DEFAULT_USER)
        `when`(state!!.users).thenReturn(mutableListOf(User.DEFAULT_USER))
        `when`(state!!.appRegistrations).thenReturn(mutableListOf(AppRegistration(
                "TEST_CLIENT_ID",
                "TEST_CLIENT_SECRET",
                mutableListOf("http://TEST_REDIRECT_HOST.com/REDIRECT_PATH"),
                mutableMapOf("testCode" to User.DEFAULT_USER))))
    }

    @Test
    fun dialog() {
        mockMvc!!.perform(
                get("/dialog/oauth")
                        .param("response_type", "code")
                        .param("client_id", "TEST_CLIENT_ID")
                        .param("scope", "user:email read:user")
                        .param("state", "TEST_STATE")
                        .param("redirect_uri", "http://TEST_REDIRECT_HOST.com/REDIRECT_PATH"))
                .andExpect(status().isFound)
    }

    @Test
    fun accessToken() {
        mockMvc!!.perform(
                post("/oauth/access_token")
                        .param("grant_type", "code")
                        .param("code", "testCode")
                        .param("redirect_uri", "http://TEST_REDIRECT_HOST.com/REDIRECT_PATH")
                        .param("client_id", "TEST_CLIENT_ID")
                        .param("client_secret", "TEST_CLIENT_SECRET"))
                .andExpect(status().isOk)
    }

    @Test
    fun me() {
        mockMvc!!.perform(
                get("/me")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJlZWZjNTBlZC1mMDYwLTRkODEtOTQ4OS01YWQzOWE5YmNjMGUiLCJpYXQiOjE1Njk3MDAwNjMsInN1YiI6IjAiLCJpc3MiOiJNZSJ9.g51ey6VFTE01LfXZbxGhdOYfunHPDPb5kjpzd_zokdc"))
                .andExpect(status().isOk)
                .andExpect(content().json(this::class.java.classLoader.getResource("meResponse.json")!!.readText(), true))
        verify(state, only())?.users
    }

    @Test
    fun unauthorizedMe() {
        mockMvc!!.perform(
                get("/me"))
                .andExpect(status().isUnauthorized)
        verify(state, never())?.users
    }

}
