package com.cineplex.app.ui.screens.seats

import androidx.lifecycle.SavedStateHandle
import com.cineplex.app.data.model.*
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
class SeatSelectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeMovieRepository : MovieRepository() {
        var cancelBookingCalledWithId: Int? = null
        var createBookingCalled = false
        var loadSeatsCallCount = 0

        val seatsList = listOf(
            ShowSeatDto(
                id = 1,
                showId = 10,
                seatId = 1,
                rowLabel = "A",
                seatNumber = 1,
                seatCode = "A1",
                seatType = "STANDARD",
                status = "AVAILABLE",
                price = 150.0
            ),
            ShowSeatDto(
                id = 2,
                showId = 10,
                seatId = 2,
                rowLabel = "A",
                seatNumber = 2,
                seatCode = "A2",
                seatType = "STANDARD",
                status = "AVAILABLE",
                price = 150.0
            ),
            ShowSeatDto(
                id = 3,
                showId = 10,
                seatId = 3,
                rowLabel = "A",
                seatNumber = 3,
                seatCode = "A3",
                seatType = "STANDARD",
                status = "BOOKED",
                price = 150.0
            ),
            ShowSeatDto(
                id = 4,
                showId = 10,
                seatId = 4,
                rowLabel = "A",
                seatNumber = 4,
                seatCode = "A4",
                seatType = "STANDARD",
                status = "HELD",
                price = 150.0
            ),
            ShowSeatDto(
                id = 5,
                showId = 10,
                seatId = 5,
                rowLabel = "A",
                seatNumber = 5,
                seatCode = "A5",
                seatType = "STANDARD",
                status = "BLOCKED",
                price = 150.0
            ),
        )

        override suspend fun getShowSeats(showId: Int): UiState<ShowDetailsWithSeatsDto> {
            loadSeatsCallCount++
            return UiState.Success(
                ShowDetailsWithSeatsDto(
                    show = ShowDto(
                        id = showId,
                        movieId = 10,
                        screenId = 1,
                        startTime = "2026-10-25T18:00:00",
                        endTime = "2026-10-25T20:30:00",
                        basePrice = 150.0,
                        isActive = true
                    ),
                    seats = seatsList
                )
            )
        }

        override suspend fun createBooking(showId: Int, seatIds: List<Int>): UiState<BookingDto> {
            createBookingCalled = true
            return UiState.Success(
                BookingDto(
                    id = 101,
                    userId = 1,
                    showId = showId,
                    bookingReference = "RNPS-TEST-101",
                    status = "PENDING",
                    totalAmount = seatIds.size * 150.0,
                    convenienceFee = 0.0,
                    totalSeats = seatIds.size,
                    createdAt = "2026-10-25T18:00:00",
                    seats = seatIds.map {
                        BookingSeatDto(
                            id = it,
                            seatId = it,
                            seatCode = "A$it",
                            rowLabel = "A",
                            seatNumber = it,
                            price = 150.0
                        )
                    },
                    snacks = emptyList()
                )
            )
        }

        override suspend fun cancelBooking(bookingId: Int): UiState<Map<String, String>> {
            cancelBookingCalledWithId = bookingId
            return UiState.Success(mapOf("message" to "Booking cancelled"))
        }
    }

    private lateinit var fakeRepo: FakeMovieRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeMovieRepository()
        savedStateHandle = SavedStateHandle(mapOf("showId" to 10))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleSeatSelection_updatesSelectedSeatsInMemoryOnly() = runTest(testDispatcher) {
        val vm = SeatSelectionViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        val seat1 = fakeRepo.seatsList[0]
        val seat2 = fakeRepo.seatsList[1]

        // Select seat 1
        vm.toggleSeatSelection(seat1)
        assertEquals(setOf(1), vm.selectedSeatIds.value)
        assertFalse(fakeRepo.createBookingCalled)

        // Select seat 2
        vm.toggleSeatSelection(seat2)
        assertEquals(setOf(1, 2), vm.selectedSeatIds.value)

        // Calculate total
        val total = vm.calculateTotal(fakeRepo.seatsList, showBasePrice = 150.0)
        assertEquals(300.0, total, 0.001)

        // Deselect seat 1
        vm.toggleSeatSelection(seat1)
        assertEquals(setOf(2), vm.selectedSeatIds.value)
    }

    @Test
    fun toggleSeatSelection_ignoresBookedBlockedAndHeldSeats() = runTest(testDispatcher) {
        val vm = SeatSelectionViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        val bookedSeat = fakeRepo.seatsList[2]
        val heldSeat = fakeRepo.seatsList[3]
        val blockedSeat = fakeRepo.seatsList[4]

        vm.toggleSeatSelection(bookedSeat)
        vm.toggleSeatSelection(heldSeat)
        vm.toggleSeatSelection(blockedSeat)

        assertTrue(vm.selectedSeatIds.value.isEmpty())
    }

    @Test
    fun confirmBooking_successSetsActiveBookingId() = runTest(testDispatcher) {
        val vm = SeatSelectionViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        vm.toggleSeatSelection(fakeRepo.seatsList[0])
        vm.confirmBooking(showBasePrice = 150.0)
        advanceUntilIdle()

        assertTrue(vm.bookingState.value is UiState.Success)
        assertEquals(101, vm.activeBookingId)
    }

    @Test
    fun cancelActiveBooking_invokesRepositoryAndClearsActiveId() = runTest(testDispatcher) {
        val vm = SeatSelectionViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        vm.toggleSeatSelection(fakeRepo.seatsList[0])
        vm.confirmBooking(showBasePrice = 150.0)
        advanceUntilIdle()

        assertEquals(101, vm.activeBookingId)

        // User presses Back or cancels
        vm.releaseScope = this
        val initialLoadCount = fakeRepo.loadSeatsCallCount
        vm.cancelActiveBooking()
        advanceUntilIdle()

        assertEquals(101, fakeRepo.cancelBookingCalledWithId)
        assertNull(vm.activeBookingId)
        assertTrue(fakeRepo.loadSeatsCallCount > initialLoadCount)
    }
}
