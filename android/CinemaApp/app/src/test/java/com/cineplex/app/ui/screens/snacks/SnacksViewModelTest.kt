package com.cineplex.app.ui.screens.snacks

import androidx.lifecycle.SavedStateHandle
import com.cineplex.app.data.model.BookingDto
import com.cineplex.app.data.model.BookingSnackDto
import com.cineplex.app.data.model.SnackDto
import com.cineplex.app.data.model.SnackItemRequest
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
class SnacksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeMovieRepository : MovieRepository() {
        var addSnacksCalledWith: List<SnackItemRequest>? = null
        var shouldAddSnacksFail = false

        val testSnacks = listOf(
            SnackDto(id = 1, name = "Caramel Popcorn", price = 120.0, category = "POPCORN", isAvailable = true),
            SnackDto(id = 2, name = "Cold Coffee", price = 80.0, category = "BEVERAGES", isAvailable = true),
            SnackDto(id = 3, name = "Nachos with Cheese", price = 150.0, category = "SNACKS", isAvailable = true),
        )

        var existingBookingSnacks: List<BookingSnackDto> = emptyList()

        override fun getCachedSnacks(): List<SnackDto>? = testSnacks

        override suspend fun getSnacks(forceRefresh: Boolean): UiState<List<SnackDto>> {
            return UiState.Success(testSnacks)
        }

        override suspend fun getBooking(bookingId: Int): UiState<BookingDto> {
            return UiState.Success(
                BookingDto(
                    id = bookingId,
                    userId = 1,
                    showId = 10,
                    bookingReference = "RNPS-TEST-42",
                    status = "PENDING",
                    totalAmount = 300.0,
                    convenienceFee = 0.0,
                    totalSeats = 2,
                    createdAt = "2026-10-25T18:00:00",
                    seats = emptyList(),
                    snacks = existingBookingSnacks
                )
            )
        }

        override suspend fun addSnacksToBooking(bookingId: Int, items: List<SnackItemRequest>): UiState<BookingDto> {
            addSnacksCalledWith = items
            return if (shouldAddSnacksFail) {
                UiState.Error("Failed to save snacks to booking", "500")
            } else {
                UiState.Success(
                    BookingDto(
                        id = bookingId,
                        userId = 1,
                        showId = 10,
                        bookingReference = "RNPS-TEST-42",
                        status = "PENDING",
                        totalAmount = 500.0,
                        convenienceFee = 0.0,
                        totalSeats = 2,
                        createdAt = "2026-10-25T18:00:00",
                        seats = emptyList(),
                        snacks = items.map {
                            BookingSnackDto(
                                id = it.snackId,
                                snackId = it.snackId,
                                snackName = "Snack ${it.snackId}",
                                quantity = it.quantity,
                                unitPrice = 100.0,
                                totalPrice = it.quantity * 100.0
                            )
                        }
                    )
                )
            }
        }
    }

    private lateinit var fakeRepo: FakeMovieRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeMovieRepository()
        savedStateHandle = SavedStateHandle(mapOf("bookingId" to 42))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun incrementQuantity_immediatelyUpdatesCartAndSubtotals() = runTest(testDispatcher) {
        val vm = SnacksViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        // Initially empty
        assertTrue(vm.quantities.value.isEmpty())
        assertEquals(0, vm.getTotalItemsCount())

        // Increment Popcorn (id=1, price=120)
        vm.incrementQuantity(1)
        assertEquals(1, vm.quantities.value[1])
        assertEquals(1, vm.getTotalItemsCount())

        val selectedList = vm.getSelectedSnacksList(fakeRepo.testSnacks)
        assertEquals(1, selectedList.size)
        assertEquals("Caramel Popcorn", selectedList[0].snack.name)
        assertEquals(1, selectedList[0].quantity)
        assertEquals(120.0, selectedList[0].unitPrice, 0.001)
        assertEquals(120.0, selectedList[0].subtotal, 0.001)
        assertEquals(120.0, vm.calculateTotal(fakeRepo.testSnacks), 0.001)

        // Increment again
        vm.incrementQuantity(1)
        assertEquals(2, vm.quantities.value[1])
        val updatedList = vm.getSelectedSnacksList(fakeRepo.testSnacks)
        assertEquals(240.0, updatedList[0].subtotal, 0.001)
        assertEquals(240.0, vm.calculateTotal(fakeRepo.testSnacks), 0.001)
    }

    @Test
    fun multipleSnacksAndQuantities_computesSubtotalsAndTotalsAccurately() = runTest(testDispatcher) {
        val vm = SnacksViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        vm.incrementQuantity(1) // Popcorn: 120 * 1 = 120
        vm.incrementQuantity(2) // Coffee: 80 * 1 = 80
        vm.incrementQuantity(2) // Coffee: 80 * 2 = 160

        val list = vm.getSelectedSnacksList(fakeRepo.testSnacks)
        assertEquals(2, list.size)

        val popcorn = list.first { it.snack.id == 1 }
        assertEquals(1, popcorn.quantity)
        assertEquals(120.0, popcorn.subtotal, 0.001)

        val coffee = list.first { it.snack.id == 2 }
        assertEquals(2, coffee.quantity)
        assertEquals(160.0, coffee.subtotal, 0.001)

        assertEquals(280.0, vm.calculateTotal(fakeRepo.testSnacks), 0.001)
        assertEquals(3, vm.getTotalItemsCount())
    }

    @Test
    fun decrementQuantity_and_removeSnack_immediatelyRemovesFromCart() = runTest(testDispatcher) {
        val vm = SnacksViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        vm.incrementQuantity(1)
        vm.incrementQuantity(2)
        vm.incrementQuantity(2)

        // Decrement coffee from 2 to 1
        vm.decrementQuantity(2)
        assertEquals(1, vm.quantities.value[2])

        // Remove coffee completely
        vm.removeSnack(2)
        assertNull(vm.quantities.value[2])

        // Decrement popcorn from 1 -> removed
        vm.decrementQuantity(1)
        assertNull(vm.quantities.value[1])

        assertTrue(vm.getSelectedSnacksList(fakeRepo.testSnacks).isEmpty())
        assertEquals(0.0, vm.calculateTotal(fakeRepo.testSnacks), 0.001)
    }

    @Test
    fun loadBookingSnacks_restoresSelectionsFromActiveBooking() = runTest(testDispatcher) {
        fakeRepo.existingBookingSnacks = listOf(
            BookingSnackDto(
                id = 1,
                snackId = 1,
                snackName = "Caramel Popcorn",
                quantity = 3,
                unitPrice = 120.0,
                totalPrice = 360.0
            )
        )

        val vm = SnacksViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        // Restored from booking
        assertEquals(3, vm.quantities.value[1])
        assertEquals(3, vm.getTotalItemsCount())
        assertEquals(360.0, vm.calculateTotal(fakeRepo.testSnacks), 0.001)
    }

    @Test
    fun submitSnacks_failure_setsErrorStateWithoutFalseSuccess() = runTest(testDispatcher) {
        fakeRepo.shouldAddSnacksFail = true
        val vm = SnacksViewModel(fakeRepo, savedStateHandle)
        advanceUntilIdle()

        vm.incrementQuantity(1)
        vm.submitSnacks()
        advanceUntilIdle()

        assertTrue(vm.saveState.value is UiState.Error)
        val error = vm.saveState.value as UiState.Error
        assertEquals("Failed to save snacks to booking", error.message)
    }
}
