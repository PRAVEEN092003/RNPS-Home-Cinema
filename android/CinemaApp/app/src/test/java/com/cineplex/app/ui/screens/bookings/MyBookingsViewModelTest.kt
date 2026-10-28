package com.cineplex.app.ui.screens.bookings

import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.model.BookingSeatDto
import com.cineplex.app.data.model.MovieDto
import com.cineplex.app.data.model.ScreenDto
import com.cineplex.app.data.model.ShowDto
import com.cineplex.app.data.repository.MovieRepository
import com.cineplex.app.util.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyBookingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeMovieRepository : MovieRepository() {
        var bookingsResult: UiState<List<BookingDto>> = UiState.Success(emptyList())
        var lastStatusQueried: String? = null
        var callCount = 0

        override suspend fun getMyBookings(status: String?): UiState<List<BookingDto>> {
            callCount++
            lastStatusQueried = status
            return bookingsResult
        }
    }

    private lateinit var fakeRepo: FakeMovieRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeMovieRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleBooking(id: Int, status: String, ref: String = "RNPS-$id"): BookingDto {
        return BookingDto(
            id = id,
            userId = 1,
            showId = 10,
            bookingReference = ref,
            status = status,
            totalAmount = 250.0,
            convenienceFee = 0.0,
            totalSeats = 1,
            createdAt = "2026-10-28T10:00:00",
            show = ShowDto(
                id = 10,
                movieId = 1,
                screenId = 1,
                startTime = "2026-10-28T18:00:00",
                endTime = "2026-10-28T20:30:00",
                basePrice = 250.0,
                isActive = true,
                movie = MovieDto(
                    id = 1,
                    title = "Leo",
                    language = "Tamil",
                    durationMinutes = 164,
                    isActive = true,
                    createdAt = "2026-10-01T00:00:00"
                ),
                screen = ScreenDto(
                    id = 1,
                    name = "Audi 1",
                    totalSeats = 50,
                    isActive = true
                )
            ),
            seats = listOf(
                BookingSeatDto(
                    id = 1,
                    seatId = 101,
                    seatCode = "A1",
                    rowLabel = "A",
                    seatNumber = 1,
                    price = 250.0
                )
            )
        )
    }

    @Test
    fun bookingsState_initiallyLoadsBookingsSuccessfully() = runTest(testDispatcher) {
        val bookings = listOf(
            sampleBooking(1, "CONFIRMED"),
            sampleBooking(2, "PENDING")
        )
        fakeRepo.bookingsResult = UiState.Success(bookings)

        val viewModel = MyBookingsViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.bookingsState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(2, data.size)
        assertEquals(1, data[0].id)
        assertEquals("CONFIRMED", data[0].status)
    }

    @Test
    fun bookingsState_whenRepositoryReturnsEmpty_emitsSuccessEmptyList() = runTest(testDispatcher) {
        fakeRepo.bookingsResult = UiState.Success(emptyList())

        val viewModel = MyBookingsViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.bookingsState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertTrue(data.isEmpty())
    }

    @Test
    fun bookingsState_whenRepositoryReturnsError_emitsErrorState() = runTest(testDispatcher) {
        fakeRepo.bookingsResult = UiState.Error("Server error", "500")

        val viewModel = MyBookingsViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.bookingsState.value
        assertTrue(state is UiState.Error)
        assertEquals("Server error", (state as UiState.Error).message)
    }

    @Test
    fun selectTab_updatesSelectedTabAndQueriesRepositoryWithStatus() = runTest(testDispatcher) {
        val viewModel = MyBookingsViewModel(fakeRepo)
        advanceUntilIdle()
        assertEquals(0, viewModel.selectedTab.value)
        assertNull(fakeRepo.lastStatusQueried)

        viewModel.selectTab(1)
        advanceUntilIdle()
        assertEquals(1, viewModel.selectedTab.value)
        assertEquals("PENDING", fakeRepo.lastStatusQueried)

        viewModel.selectTab(2)
        advanceUntilIdle()
        assertEquals(2, viewModel.selectedTab.value)
        assertEquals("CONFIRMED", fakeRepo.lastStatusQueried)

        viewModel.selectTab(3)
        advanceUntilIdle()
        assertEquals(3, viewModel.selectedTab.value)
        assertEquals("CANCELLED", fakeRepo.lastStatusQueried)

        viewModel.selectTab(0)
        advanceUntilIdle()
        assertEquals(0, viewModel.selectedTab.value)
        assertNull(fakeRepo.lastStatusQueried)
    }

    @Test
    fun bookingsTabFiltering_correctlyFiltersAllCategories() {
        val bPending = sampleBooking(1, "PENDING")
        val bConfirmed = sampleBooking(2, "CONFIRMED")
        val bBooked = sampleBooking(3, "BOOKED")
        val bCancelled = sampleBooking(4, "CANCELLED")
        val allBookings = listOf(bPending, bConfirmed, bBooked, bCancelled)

        // Tab 0: All
        val tab0Filtered = allBookings
        assertEquals(4, tab0Filtered.size)

        // Tab 1: Pending
        val tab1Filtered = allBookings.filter { it.status == "PENDING" }
        assertEquals(1, tab1Filtered.size)
        assertEquals(1, tab1Filtered[0].id)

        // Tab 2: Confirmed / Booked
        val tab2Filtered = allBookings.filter { it.status == "CONFIRMED" || it.status == "BOOKED" }
        assertEquals(2, tab2Filtered.size)
        assertEquals(setOf(2, 3), tab2Filtered.map { it.id }.toSet())

        // Tab 3: Cancelled / Rejected / Failed
        val tab3Filtered = allBookings.filter { it.status == "CANCELLED" || it.status == "REJECTED" || it.payment?.status == "FAILED" }
        assertEquals(1, tab3Filtered.size)
        assertEquals(4, tab3Filtered[0].id)
    }

    @Test
    fun bookingDto_jsonDeserialization_succeedsWithoutError() {
        val jsonStr = """
            {
                "id": 42,
                "user_id": 7,
                "show_id": 10,
                "booking_reference": "RNPS-20261028-42",
                "status": "CONFIRMED",
                "total_amount": 350.0,
                "convenience_fee": 0.0,
                "total_seats": 1,
                "created_at": "2026-10-28T12:00:00",
                "ticket_amount": 350.0,
                "snacks_amount": 0.0,
                "seats": [
                    {
                        "id": 100,
                        "seat_id": 5,
                        "seat_code": "B3",
                        "row_label": "B",
                        "seat_number": 3,
                        "price": 350.0
                    }
                ]
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val dto = json.decodeFromString<BookingDto>(jsonStr)

        assertEquals(42, dto.id)
        assertEquals(7, dto.userId)
        assertEquals("RNPS-20261028-42", dto.bookingReference)
        assertEquals("CONFIRMED", dto.status)
        assertEquals(350.0, dto.totalAmount, 0.001)
        assertEquals(1, dto.seats.size)
        assertEquals("B3", dto.seats[0].seatCode)
    }
}
