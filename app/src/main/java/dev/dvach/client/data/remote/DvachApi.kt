package dev.dvach.client.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import dev.dvach.client.data.remote.dto.BoardDto
import dev.dvach.client.data.remote.dto.ThreadsPageDto

/**
 * Актуальный (2026) JSON API движка Makaba.
 *
 * Старые эндпоинты, на которых работало приложение 2019 года, отключены:
 *  - `/boards.json`                        -> 404
 *  - `/makaba/mobile.fcgi?task=get_boards` -> 404
 *  - `/makaba/posting.fcgi`                -> 404 (постинг переехал на POST /user/posting)
 */
interface DvachApi {

    @GET("api/mobile/v2/boards")
    suspend fun boards(): List<BoardDto>

    @GET("{board}/index.json")
    suspend fun index(
        @Path("board") board: String,
        @Query("page") page: Int = 0,
    ): ThreadsPageDto

    @GET("{board}/res/{thread}.json")
    suspend fun thread(
        @Path("board") board: String,
        @Path("thread") thread: Int,
    ): ThreadsPageDto
}
