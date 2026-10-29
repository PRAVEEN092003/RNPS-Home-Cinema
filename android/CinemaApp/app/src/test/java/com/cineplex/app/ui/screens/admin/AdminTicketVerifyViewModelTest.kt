package com.cineplex.app.ui.screens.admin

import com.cineplex.app.data.model.TicketVerificationDto
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminTicketVerifyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeMovieRepository : MovieRepository() {
        var verifyResult: UiState<TicketVerificationDto> = UiState.Success(
            TicketVerificationDto(
                isValid = true,
                ticketCode = "TKT-TEST-001",
                bookingReference = "BKG-TEST-001",
                userName = "Admin Tester",
                customerEmail = "tester@cinema.test",
                movieTitle = "Master",
                screenName = "Screen 1",
                showTime = "07:30 PM",
                seats = listOf("A1", "A2"),
                ticketCount = 2,
                bookingStatus = "CONFIRMED",
                paymentStatus = "PAID",
                validityStatus = "VALID",
                issuedAt = "2026-10-29T14:00:00Z",
                isUsed = false
            )
        )
        var checkInResult: UiState<TicketVerificationDto> = UiState.Success(
            TicketVerificationDto(
                isValid = true,
                ticketCode = "TKT-TEST-001",
                bookingReference = "BKG-TEST-001",
                userName = "Admin Tester",
                customerEmail = "tester@cinema.test",
                movieTitle = "Master",
                screenName = "Screen 1",
                showTime = "07:30 PM",
                seats = listOf("A1", "A2"),
                ticketCount = 2,
                bookingStatus = "CONFIRMED",
                paymentStatus = "PAID",
                validityStatus = "ALREADY_USED",
                issuedAt = "2026-10-29T14:00:00Z",
                isUsed = true,
                usedAt = "2026-10-29T14:05:00Z"
            )
        )

        var lastVerifiedCode: String? = null
        var lastCheckedInCode: String? = null

        override suspend fun verifyTicket(code: String): UiState<TicketVerificationDto> {
            lastVerifiedCode = code
            return verifyResult
        }

        override suspend fun checkInTicket(code: String): UiState<TicketVerificationDto> {
            lastCheckedInCode = code
            return checkInResult
        }
    }

    private lateinit var fakeRepo: FakeMovieRepository
    private lateinit var viewModel: AdminTicketVerifyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeMovieRepository()
        viewModel = AdminTicketVerifyViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCameraOpenAndClose() {
        assertFalse(viewModel.isCameraOpen.value)
        viewModel.openCamera()
        assertTrue(viewModel.isCameraOpen.value)
        viewModel.closeCamera()
        assertFalse(viewModel.isCameraOpen.value)
    }

    @Test
    fun testOnQrScanned_triggersVerificationAndClosesCamera() = runTest(testDispatcher) {
        viewModel.openCamera()
        assertTrue(viewModel.isCameraOpen.value)

        viewModel.onQrScanned("TKT-TEST-001")
        assertFalse(viewModel.isCameraOpen.value)
        assertEquals("TKT-TEST-001", viewModel.codeInput.value)

        advanceUntilIdle()

        assertEquals("TKT-TEST-001", fakeRepo.lastVerifiedCode)
        val verifyState = viewModel.verifyState.value
        assertTrue(verifyState is UiState.Success)
        val ticket = (verifyState as UiState.Success).data
        assertTrue(ticket.isValid)
        assertEquals("VALID", ticket.validityStatus)
        assertEquals("Master", ticket.movieTitle)
    }

    @Test
    fun testCheckInTicket_updatesVerificationStateToUsed() = runTest(testDispatcher) {
        viewModel.checkInTicket("TKT-TEST-001")
        advanceUntilIdle()

        assertEquals("TKT-TEST-001", fakeRepo.lastCheckedInCode)
        val checkInState = viewModel.checkInState.value
        assertTrue(checkInState is UiState.Success)
        val checkedInTicket = (checkInState as UiState.Success).data
        assertTrue(checkedInTicket.isUsed)
        assertEquals("ALREADY_USED", checkedInTicket.validityStatus)

        // verifyState should also reflect the checked in ticket immediately
        val verifyState = viewModel.verifyState.value
        assertTrue(verifyState is UiState.Success)
        assertTrue((verifyState as UiState.Success).data.isUsed)
    }

    @Test
    fun testResetVerification_clearsState() {
        viewModel.codeInput.value = "TKT-123"
        viewModel.resetVerification()
        assertEquals("", viewModel.codeInput.value)
        assertNull(viewModel.verifyState.value)
        assertNull(viewModel.checkInState.value)
    }
}
