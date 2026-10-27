package dev.dvach.client.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
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

    /**
     * Первая страница доски. `?page=N` движок больше не понимает: параметр
     * игнорируется и всегда отдаётся первая страница, поэтому страницы
     * запрашиваются путём `/{board}/{N}.json` (см. [indexPage]).
     */
    @GET("{board}/index.json")
    suspend fun index(@Path("board") board: String): ThreadsPageDto

    /** Страница доски, `page` — с единицы, как в вебе: `/b/2.json`. */
    @GET("{board}/{page}.json")
    suspend fun indexPage(
        @Path("board") board: String,
        @Path("page") page: Int,
    ): ThreadsPageDto

    @GET("{board}/res/{thread}.json")
    suspend fun thread(
        @Path("board") board: String,
        @Path("thread") thread: Int,
    ): ThreadsPageDto
}
