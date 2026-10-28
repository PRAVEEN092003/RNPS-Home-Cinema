package com.cineplex.app.ui.screens.auth

import com.cineplex.app.data.model.TokenResponse
import com.cineplex.app.data.model.UserDto
import com.cineplex.app.data.repository.AuthRepository
import com.cineplex.app.util.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository : AuthRepository() {
        var loginCallCount = 0
        var registerCallCount = 0

        override fun warmUpServer() {
            // No-op for tests
        }

        override suspend fun login(email: String, password: String): UiState<TokenResponse> {
            loginCallCount++
            return if (email == "admin@rnps.com" && password == "admin123") {
                UiState.Success(
                    TokenResponse(
                        accessToken = "test_token",
                        tokenType = "bearer",
                        user = UserDto(
                            id = 1,
                            name = "Admin",
                            email = "admin@rnps.com",
                            role = "ADMIN",
                            isActive = true,
                            createdAt = "2026-10-16T00:00:00"
                        )
                    )
                )
            } else {
                UiState.Error("Incorrect email or password.", "401")
            }
        }

        override suspend fun register(name: String, email: String, password: String, phone: String?): UiState<TokenResponse> {
            registerCallCount++
            return if (email == "duplicate@example.com") {
                UiState.Error("An account with this email already exists.", "409")
            } else {
                UiState.Success(
                    TokenResponse(
                        accessToken = "reg_token",
                        tokenType = "bearer",
                        user = UserDto(
                            id = 2,
                            name = name,
                            email = email,
                            role = "USER",
                            isActive = true,
                            createdAt = "2026-10-16T00:00:00"
                        )
                    )
                )
            }
        }
    }

    private lateinit var fakeRepo: FakeAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── LoginViewModel Tests ──────────────────────────────────────────────────

    @Test
    fun loginViewModel_validation_emptyEmail_setsError() {
        val vm = LoginViewModel(fakeRepo)
        vm.emailState.value = ""
        vm.passwordState.value = "password123"

        vm.login()

        val state = vm.loginState.value
        assertTrue(state is UiState.Error)
        assertEquals("Please enter your email address.", (state as UiState.Error).message)
        assertEquals(0, fakeRepo.loginCallCount)
    }

    @Test
    fun loginViewModel_validation_emptyPassword_setsError() {
        val vm = LoginViewModel(fakeRepo)
        vm.emailState.value = "user@example.com"
        vm.passwordState.value = ""

        vm.login()

        val state = vm.loginState.value
        assertTrue(state is UiState.Error)
        assertEquals("Please enter your password.", (state as UiState.Error).message)
        assertEquals(0, fakeRepo.loginCallCount)
    }

    @Test
    fun loginViewModel_successfulLogin_updatesStateToSuccess() {
        val vm = LoginViewModel(fakeRepo)
        vm.emailState.value = "admin@rnps.com"
        vm.passwordState.value = "admin123"

        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.loginState.value
        assertTrue(state is UiState.Success)
        assertEquals("test_token", (state as UiState.Success).data.accessToken)
        assertEquals(1, fakeRepo.loginCallCount)
    }

    @Test
    fun loginViewModel_invalidCredentials_updatesStateToError() {
        val vm = LoginViewModel(fakeRepo)
        vm.emailState.value = "admin@rnps.com"
        vm.passwordState.value = "wrongpassword"

        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.loginState.value
        assertTrue(state is UiState.Error)
        assertEquals("Incorrect email or password.", (state as UiState.Error).message)
        assertEquals(1, fakeRepo.loginCallCount)
    }

    @Test
    fun loginViewModel_duplicateSubmissions_areIgnoredWhileLoading() {
        val vm = LoginViewModel(fakeRepo)
        vm.emailState.value = "admin@rnps.com"
        vm.passwordState.value = "admin123"

        vm.login()
        // While running, attempt second login
        vm.login()

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.loginCallCount)
    }

    @Test
    fun loginViewModel_resetState_clearsLoginState() {
        val vm = LoginViewModel(fakeRepo)
        vm.emailState.value = "admin@rnps.com"
        vm.passwordState.value = "admin123"
        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.resetState()
        assertNull(vm.loginState.value)
    }

    // ─── RegisterViewModel Tests ───────────────────────────────────────────────

    @Test
    fun registerViewModel_validation_emptyName_setsError() {
        val vm = RegisterViewModel(fakeRepo)
        vm.nameState.value = ""
        vm.emailState.value = "test@example.com"
        vm.passwordState.value = "password123"

        vm.register()

        val state = vm.registerState.value
        assertTrue(state is UiState.Error)
        assertEquals("Please enter your name.", (state as UiState.Error).message)
        assertEquals(0, fakeRepo.registerCallCount)
    }

    @Test
    fun registerViewModel_validation_emptyEmail_setsError() {
        val vm = RegisterViewModel(fakeRepo)
        vm.nameState.value = "Test User"
        vm.emailState.value = ""
        vm.passwordState.value = "password123"

        vm.register()

        val state = vm.registerState.value
        assertTrue(state is UiState.Error)
        assertEquals("Please enter your email.", (state as UiState.Error).message)
        assertEquals(0, fakeRepo.registerCallCount)
    }

    @Test
    fun registerViewModel_validation_shortPassword_setsError() {
        val vm = RegisterViewModel(fakeRepo)
        vm.nameState.value = "Test User"
        vm.emailState.value = "test@example.com"
        vm.passwordState.value = "123"

        vm.register()

        val state = vm.registerState.value
        assertTrue(state is UiState.Error)
        assertEquals("Password must be at least 6 characters.", (state as UiState.Error).message)
        assertEquals(0, fakeRepo.registerCallCount)
    }

    @Test
    fun registerViewModel_successfulRegistration_updatesStateToSuccess() {
        val vm = RegisterViewModel(fakeRepo)
        vm.nameState.value = "New Person"
        vm.emailState.value = "new@example.com"
        vm.passwordState.value = "secret123"

        vm.register()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.registerState.value
        assertTrue(state is UiState.Success)
        assertEquals("reg_token", (state as UiState.Success).data.accessToken)
        assertEquals(1, fakeRepo.registerCallCount)
    }

    @Test
    fun registerViewModel_duplicateRegistration_updatesStateToError() {
        val vm = RegisterViewModel(fakeRepo)
        vm.nameState.value = "Duplicate Person"
        vm.emailState.value = "duplicate@example.com"
        vm.passwordState.value = "secret123"

        vm.register()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.registerState.value
        assertTrue(state is UiState.Error)
        assertEquals("An account with this email already exists.", (state as UiState.Error).message)
        assertEquals(1, fakeRepo.registerCallCount)
    }

    @Test
    fun registerViewModel_duplicateSubmissions_areIgnoredWhileLoading() {
        val vm = RegisterViewModel(fakeRepo)
        vm.nameState.value = "New Person"
        vm.emailState.value = "new@example.com"
        vm.passwordState.value = "secret123"

        vm.register()
        // Second call while loading
        vm.register()

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.registerCallCount)
    }
}
