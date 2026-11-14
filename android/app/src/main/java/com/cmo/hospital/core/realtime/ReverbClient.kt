package com.cmo.hospital.core.realtime

import com.cmo.hospital.core.config.AppConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/** Lo que llega por el WebSocket de un canal. */
sealed class EventoTiempoReal {
    /** Suscripción aceptada: a partir de aquí llegan los eventos del canal. */
    data object Conectado : EventoTiempoReal()

    /** Evento de Laravel, p. ej. `App\Events\AmbulanciaActualizada` con su JSON. */
    data class Mensaje(val evento: String, val datos: String?) : EventoTiempoReal()
}

/**
 * Firma la suscripción a un canal privado: recibe el `socket_id` de la conexión
 * y el nombre del canal ("private-personal") y devuelve el `auth` que entrega
 * POST /broadcasting/auth, o null si el servidor la rechaza.
 */
typealias AutorizadorCanal = suspend (socketId: String, canal: String) -> String?

/**
 * Cliente mínimo de Laravel Reverb (protocolo Pusher 7). El flujo termina
 * cuando se cae la conexión; quien lo colecta decide si reintenta.
 */
class ReverbClient(
    private val urlBase: String = urlDesdeConfig(),
    private val http: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
) {
    /** Canal público (p. ej. `ambulancia`). */
    fun canal(nombre: String): Flow<EventoTiempoReal> = conectar(nombre, autorizar = null)

    /** Canal privado (p. ej. `personal`, que se suscribe como `private-personal`). */
    fun canalPrivado(nombre: String, autorizar: AutorizadorCanal): Flow<EventoTiempoReal> =
        conectar("private-$nombre", autorizar)

    private fun conectar(canal: String, autorizar: AutorizadorCanal?): Flow<EventoTiempoReal> = callbackFlow {
        val socket = http.newWebSocket(Request.Builder().url(urlBase).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return
                when (val evento = msg.texto("event")) {
                    "pusher:connection_established" -> {
                        if (autorizar == null) {
                            webSocket.send("""{"event":"pusher:subscribe","data":{"channel":"$canal"}}""")
                        } else {
                            // `data` trae {"socket_id": "..."} como texto JSON.
                            val socketId = msg.texto("data")
                                ?.let { runCatching { JsonParser.parseString(it).asJsonObject.texto("socket_id") }.getOrNull() }
                            if (socketId == null) {
                                close(IllegalStateException("Reverb no envió socket_id"))
                                return
                            }
                            launch {
                                val auth = runCatching { autorizar(socketId, canal) }.getOrNull()
                                if (auth == null) {
                                    close(IllegalStateException("Suscripción a $canal no autorizada"))
                                } else {
                                    val pedido = JsonObject().apply {
                                        addProperty("event", "pusher:subscribe")
                                        add("data", JsonObject().apply {
                                            addProperty("channel", canal)
                                            addProperty("auth", auth)
                                        })
                                    }
                                    webSocket.send(pedido.toString())
                                }
                            }
                        }
                    }
                    "pusher:ping" -> webSocket.send("""{"event":"pusher:pong","data":{}}""")
                    "pusher_internal:subscription_succeeded" -> trySend(EventoTiempoReal.Conectado)
                    "pusher:error" -> close(IllegalStateException(msg.texto("data") ?: "Error de Reverb"))
                    null -> Unit
                    else -> if (msg.texto("channel") == canal) {
                        // Pusher manda `data` como texto con JSON adentro.
                        trySend(EventoTiempoReal.Mensaje(evento, msg.texto("data")))
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }
        })
        awaitClose { socket.close(1000, null) }
    }

    companion object {
        fun urlDesdeConfig(): String {
            val esquema = if (AppConfig.REVERB_SCHEME == "https") "wss" else "ws"
            return "$esquema://${AppConfig.REVERB_HOST}:${AppConfig.REVERB_PORT}/app/${AppConfig.REVERB_APP_KEY}" +
                "?protocol=7&client=android&version=1.0&flash=false"
        }

        private fun JsonObject.texto(campo: String): String? =
            get(campo)?.takeIf { !it.isJsonNull }?.let { if (it.isJsonPrimitive) it.asString else it.toString() }
    }
}
