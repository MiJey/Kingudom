package mijey.kingudom

import android.R.attr.button
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    companion object {
        const val CHAT_SERVER_URL = "http://192.168.43.12:4747"
        const val MAX_HP = 30000

        // listen
        const val EVENT_RECRUIT = "gamepad:recruit"
        const val EVENT_RECRUIT_END = "gamepad:recruitEnd"
        const val EVENT_GAME_READY = "gamepad:gameReady"
        const val EVENT_GAME_START = "gamepad:gameStart"
        const val EVENT_GAME_END = "gamepad:gameEnd"

        const val EVENT_INFO = "gamepad:info"
        const val EVENT_CHANGE_SCENE = "gamepad:changeScene"
        const val EVENT_PLAY_SOUND = "gamepad:playSound"

        // emit
        const val EVENT_PARTICIPATE = "gamepad:participate"
        const val EVENT_KEY_DOWN = "gamepad:keyDown"

        const val SEND_UNIT = "unit"

        const val SEND_OFFENSE_UP = "attack"
        const val SEND_DEFENSE_UP = "hp"
        const val SEND_SPEED_UP = "speed"

        const val SEND_HERO = "hero"
        const val SEND_MISSILE = "missile"
        const val SEND_STOP = "stop"
    }

    private var myCamp = ""
    private var mSocket: Socket? = null
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSocket = IO.socket(CHAT_SERVER_URL)

        mSocket?.on(Socket.EVENT_CONNECT, onConnect)
        mSocket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
        mSocket?.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket?.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError)
        mSocket?.on(EVENT_RECRUIT, onRecruit)
        mSocket?.on(EVENT_RECRUIT_END, onRecruitEnd)
        mSocket?.on(EVENT_GAME_READY, onGameReady)
        mSocket?.on(EVENT_GAME_START, onGameStart)
        mSocket?.on(EVENT_GAME_END, onGameEnd)
        mSocket?.on(EVENT_INFO, onInfo)
        mSocket?.connect()

        // 아이템
        hero_button.setOnClickListener {
            sendMessage(EVENT_KEY_DOWN, SEND_HERO)
        }

        eraser_button.setOnClickListener {
            sendMessage(EVENT_KEY_DOWN, SEND_MISSILE)
        }

        stop_button.setOnClickListener {
            sendMessage(EVENT_KEY_DOWN, SEND_STOP)
        }

        // 출격
        sortie_button_1.setOnTouchListener(OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendMessage(EVENT_KEY_DOWN, SEND_UNIT)
                    return@OnTouchListener true
                }
            }
            false
        })
        
        sortie_button_2.setOnTouchListener(OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendMessage(EVENT_KEY_DOWN, SEND_UNIT)
                    return@OnTouchListener true
                }
            }
            false
        })

        // 다이얼로그
        dialog_game_end_close.setOnClickListener {
            dialog_game_end.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        mSocket?.disconnect()

        mSocket?.off(Socket.EVENT_CONNECT, onConnect)
        mSocket?.off(Socket.EVENT_DISCONNECT, onDisconnect)
        mSocket?.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket?.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError)
        mSocket?.off(EVENT_RECRUIT, onRecruit)
        mSocket?.off(EVENT_RECRUIT_END, onRecruitEnd)
        mSocket?.off(EVENT_GAME_READY, onGameReady)
        mSocket?.off(EVENT_GAME_START, onGameStart)
        mSocket?.off(EVENT_GAME_END, onGameEnd)
        mSocket?.off(EVENT_INFO, onInfo)

        super.onDestroy()
    }

    private fun sendMessage(event: String, message: String? = null) {
        Log.d(
            "yeji",
            "sendMessage. mSocket?.connected(): ${mSocket?.connected()}, event: $event, message: $message"
        )
        if (mSocket?.connected() != true) return

        if (message != null) mSocket?.emit(event, message)
        else mSocket?.emit(event)
    }

    /***********************************************************/

    private val onConnect = Emitter.Listener {
        runOnUiThread {
            if (!isConnected) {
                mSocket?.emit("setDevice", "player")
                Log.d("yeji", "connect")
                isConnected = true
            }
        }
    }

    private val onDisconnect = Emitter.Listener {
        runOnUiThread {
            Log.d("yeji", "disconnect")
            isConnected = false
        }
    }

    private val onConnectError = Emitter.Listener {
        runOnUiThread {
            Log.d("yeji", "connecting error")
            isConnected = false
        }
    }

    private val onRecruit = Emitter.Listener { args ->
        // 참가자 모집 시작
        Log.d("yeji", "onRecruit")
        sendMessage(EVENT_PARTICIPATE)
        runOnUiThread {
            dialog_game_end.visibility = View.GONE
        }
    }

    private val onRecruitEnd = Emitter.Listener { args ->
        // 참가자 모집 끝(내가 참가자가 아님)
        Log.d("yeji", "onRecruitEnd")
    }

    private val onGameReady = Emitter.Listener { args ->
        // 참가자 모집 끝(내가 참가자임)
        Log.d("yeji", "onGameReady")
        mSocket?.emit("gamepad:gameReady")
    }

    private val onGameStart = Emitter.Listener { args ->
        // 게임 시작
        Log.d("yeji", "onGameStart")
    }

    private val onGameEnd = Emitter.Listener { args ->
        // 게임 끝
        Log.d("yeji", "onGameEnd")
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                val winner = data.getString("camp")

                dialog_game_end_title.text = if (winner == myCamp) "승리!!!" else "패배..."
                dialog_game_end.visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.d("yeji", "onGameEnd error: $e")
            }
        }
    }

    private val onInfo = Emitter.Listener { args ->
        // 게임 끝
        Log.d("yeji", "onInfo")
        /*
        {
          camp: "진영 red or blue",
          money: 돈,
          castle: 캐슬 체력,
          attack: 유닛 공격력,
          hp: 유닛 체력,
          speed: 유닛 속도
        }
         */
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                val camp = data.getString("camp")
                val money = data.getInt("money")
                val castle = data.getInt("castle")

                Log.d("yeji", "onInfo. camp: $camp, money: $money, castle: $castle")

                myCamp = camp
                camp_text.text = camp
                if (camp == "red")
                    camp_text.setTextColor(ContextCompat.getColor(this, R.color.red_camp))
                else
                    camp_text.setTextColor(ContextCompat.getColor(this, R.color.blue_camp))

                hp_progress.progress = castle * 100 / MAX_HP
                hp_text.text = "${castle}/${MAX_HP}"
                money_text.text = money.toString()
            } catch (e: Exception) {
                Log.d("yeji", "onInfo error: $e")
            }
        }
    }
}
