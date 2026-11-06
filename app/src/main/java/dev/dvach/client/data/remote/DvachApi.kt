package dev.dvach.client.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import dev.dvach.client.data.remote.dto.BoardDto
import dev.dvach.client.data.remote.dto.CaptchaClickDto
import dev.dvach.client.data.remote.dto.CaptchaDto
import dev.dvach.client.data.remote.dto.CaptchaShowDto
import dev.dvach.client.data.remote.dto.PostingDto
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

    /**
     * Страница доски: `index.json` — нулевая, `1.json` — уже вторая.
     * `page` здесь как в UI, с нуля: `page = 1` вернёт `/b/1.json`.
     */
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

    /**
     * ID новой эмодзи-капчи. `thread` нужен для ответа в тред, для нового
     * треда параметр не передаётся.
     */
    @GET("api/captcha/emoji/id")
    suspend fun captchaId(
        @Query("board") board: String,
        @Query("thread") thread: Int?,
    ): CaptchaDto

    /** Первая картинка капчи (что искать) и клавиатура (из чего выбирать). */
    @GET("api/captcha/emoji/show")
    suspend fun captchaShow(@Query("id") id: String): CaptchaShowDto

    /** Клик по эмодзи: либо следующая картинка, либо `success`. */
    @POST("api/captcha/emoji/click")
    suspend fun captchaClick(@Body body: CaptchaClickDto): CaptchaShowDto

    /**
     * Отправка поста или нового треда.
     *
     * Все поля — `RequestBody`, а не строки: иначе конвертер Retrofit обернул
     * бы их в JSON-кавычки. `file[]` — имя поля, которое ждёт движок; файл
     * необязателен (`null` — часть не добавляется).
     *
     * `task=post` и `2ch_challenge` есть в форме самого сайта: без задачи
     * «доказательства работы» движок отвечает `result: 0` без пояснений.
     * `nc=1` отключает кэш, как в веб-клиенте.
     *
     * `name`, `email`, `subject`, `tags`, `usercode` уходят всегда, даже пустыми:
     * форма сайта отправляет их в любом случае, а движок на отсутствии поля
     * падает с `code = 666` («внутренняя ошибка сервера»). Пустая строка —
     * то же самое, что незаполненное поле.
     */
    @Multipart
    @POST("user/posting")
    suspend fun posting(
        @Query("nc") nc: String,
        @Part("task") task: RequestBody,
        @Part("captcha_type") captchaType: RequestBody,
        @Part("board") board: RequestBody,
        @Part("thread") thread: RequestBody?,
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("subject") subject: RequestBody,
        @Part("tags") tags: RequestBody,
        @Part("usercode") usercode: RequestBody,
        @Part("comment") comment: RequestBody,
        @Part("emoji_captcha_id") captchaId: RequestBody,
        @Part("2ch_challenge") challenge: RequestBody?,
        @Part file: MultipartBody.Part?,
    ): PostingDto}
