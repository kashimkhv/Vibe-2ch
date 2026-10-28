package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Test
import dev.dvach.client.domain.model.Board
import dev.dvach.client.ui.boards.BoardsUiState

class BoardsUiStateTest {

    private fun board(id: String, name: String = id, category: String = "Разное") = Board(
        id = id,
        name = name,
        category = category,
        info = "",
        threadsPerPage = 10,
        maxPages = 10,
        bumpLimit = 500,
        maxComment = 20_000,
        enablePosting = true,
        enableNames = true,
        enableSubject = true,
        enableTrips = true,
        enableFlags = true,
        enableIcons = true,
        enableLikes = true,
        enableSage = true,
        fileTypes = emptyList(),
    )

    private val boards = listOf(
        board("b", "Бред"),
        board("tech", "Технологии"),
        board("mu", "Музыка"),
    )

    @Test
    fun `pinned boards keep the order they were pinned in`() {
        val state = BoardsUiState(
            isLoading = false,
            boards = boards,
            pinnedBoardIds = listOf("mu", "b"),
        )
        assertEquals(listOf("mu", "b"), state.pinnedBoards.map { it.id })
    }

    @Test
    fun `pinned boards follow the search filter`() {
        val state = BoardsUiState(
            isLoading = false,
            boards = boards,
            pinnedBoardIds = listOf("mu", "b"),
            query = "муз",
        )
        assertEquals(listOf("mu"), state.pinnedBoards.map { it.id })
        assertEquals(listOf("mu"), state.visibleBoards.map { it.id })
    }

    @Test
    fun `pinned boards survive when a board disappears from the server`() {
        val state = BoardsUiState(
            isLoading = false,
            boards = boards.filterNot { it.id == "mu" },
            pinnedBoardIds = listOf("mu", "tech"),
        )
        assertEquals(listOf("tech"), state.pinnedBoards.map { it.id })
    }
}
