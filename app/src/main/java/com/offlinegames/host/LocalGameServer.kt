package com.offlinegames.host

import android.content.Context
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.Frame
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LocalGameServer(private val context: Context) {
    val port: Int = 3000
    private var engine: ApplicationEngine? = null
    private var lobbyCode: String = ""
    private val state = TicTacToeState()
    private val sessions = ConcurrentHashMap<String, io.ktor.server.websocket.DefaultWebSocketServerSession>()

    fun start() {
        if (engine != null) return
        engine = embeddedServer(CIO, port = port) {
            routing {
                staticResources("/", "web")
                get("/api/status") { call.respondText("online") }
                post("/api/lobby/create") {
                    lobbyCode = randomCode()
                    state.reset()
                    call.respondText(lobbyCode)
                }
                post("/api/lobby/reset") {
                    state.reset()
                    broadcast(GameMessage("reset", ""))
                    call.respondText("ok")
                }
                webSocket("/ws") {
                    val id = UUID.randomUUID().toString()
                    sessions[id] = this
                    send(Frame.Text(Json.encodeToString(GameMessage("state", state.toJson()))))
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) handleMessage(frame.readText())
                        }
                    } finally {
                        sessions.remove(id)
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() { engine?.stop(); engine = null }
    fun isRunning() = engine != null
    fun currentLobbyCode() = lobbyCode
    fun createLobby(): String { lobbyCode = randomCode(); state.reset(); return lobbyCode }

    private suspend fun handleMessage(raw: String) {
        val msg = Json.decodeFromString<ClientMove>(raw)
        if (msg.type == "move" && msg.lobbyCode == lobbyCode && state.play(msg.index, msg.symbol)) {
            broadcast(GameMessage("state", state.toJson()))
        } else if (msg.type == "reset" && msg.isHost) {
            state.reset(); broadcast(GameMessage("state", state.toJson()))
        }
    }

    private fun randomCode(): String = UUID.randomUUID().toString().take(6).uppercase()

    private suspend fun broadcast(message: GameMessage) {
        val encoded = Json.encodeToString(message)
        sessions.values.forEach { it.send(Frame.Text(encoded)) }
    }
}

@Serializable
data class ClientMove(val type: String, val lobbyCode: String, val index: Int = -1, val symbol: String = "", val isHost: Boolean = false)
@Serializable
data class GameMessage(val type: String, val payload: String)

class TicTacToeState {
    private val cells = MutableList(9) { "" }
    private var turn = "X"
    private var winner = ""

    fun play(index: Int, symbol: String): Boolean {
        if (winner.isNotEmpty() || index !in 0..8 || cells[index].isNotEmpty() || symbol != turn) return false
        cells[index] = symbol
        winner = checkWinner()
        turn = if (turn == "X") "O" else "X"
        return true
    }

    fun reset() {
        for (i in cells.indices) cells[i] = ""
        turn = "X"
        winner = ""
    }

    fun toJson(): String = Json.encodeToString(TicTacToeSnapshot(cells, turn, winner))

    private fun checkWinner(): String {
        val lines = listOf(
            listOf(0,1,2), listOf(3,4,5), listOf(6,7,8),
            listOf(0,3,6), listOf(1,4,7), listOf(2,5,8),
            listOf(0,4,8), listOf(2,4,6)
        )
        for (line in lines) {
            val (a,b,c) = line
            if (cells[a].isNotEmpty() && cells[a] == cells[b] && cells[b] == cells[c]) return cells[a]
        }
        return if (cells.none { it.isEmpty() }) "DRAW" else ""
    }
}

@Serializable
data class TicTacToeSnapshot(val cells: List<String>, val turn: String, val winner: String)
