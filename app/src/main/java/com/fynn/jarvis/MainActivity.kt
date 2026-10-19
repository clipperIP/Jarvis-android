package com.fynn.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var transcript: TextView
    private lateinit var prompt: EditText
    private lateinit var apiKey: EditText
    private lateinit var tts: TextToSpeech
    private var ready = false
    private val blue = Color.rgb(80, 210, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        window.statusBarColor = Color.rgb(3, 12, 25)
        window.navigationBarColor = Color.rgb(3, 12, 25)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 20)
            setBackgroundColor(Color.rgb(3, 12, 25))
        }
        val title = TextView(this).apply {
            text = "J.A.R.V.I.S."
            textSize = 30f
            setTextColor(blue)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(this).apply {
            text = "PERSONAL AI • ANDROID EDITION"
            textSize = 11f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY)
        }, LinearLayout.LayoutParams(-1, 44))

        apiKey = EditText(this).apply {
            hint = "Gemini API key (stored on this phone)"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setText(getPreferences(0).getString("gemini_key", ""))
            setPadding(14, 8, 14, 8)
            setBackgroundColor(Color.rgb(15, 30, 48))
        }
        root.addView(apiKey, LinearLayout.LayoutParams(-1, 52))
        val save = button("SAVE API KEY") { 
            getPreferences(0).edit().putString("gemini_key", apiKey.text.toString().trim()).apply()
            status("API key saved on this phone.")
        }
        root.addView(save, LinearLayout.LayoutParams(-1, 46).apply { topMargin = 8 })

        transcript = TextView(this).apply {
            text = "Ready when you are, Fynn. Add your Gemini API key, then type or speak."
            textSize = 16f; setTextColor(Color.WHITE); setPadding(14, 14, 14, 14)
            setBackgroundColor(Color.rgb(10, 24, 40))
        }
        val scroll = ScrollView(this).apply { addView(transcript) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 16; bottomMargin = 12 })
        prompt = EditText(this).apply {
            hint = "Talk to J.A.R.V.I.S..."
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(15, 30, 48)); setPadding(14, 8, 14, 8)
        }
        root.addView(prompt, LinearLayout.LayoutParams(-1, 54))
        root.addView(button("SEND MESSAGE") { ask(prompt.text.toString()); prompt.setText("") }, LinearLayout.LayoutParams(-1, 48).apply { topMargin = 8 })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(button("🎙 SPEAK") { listen() }, LinearLayout.LayoutParams(0, 50, 1f).apply { rightMargin = 5; topMargin = 8 })
        row.addView(button("CONTACTS") { showContacts() }, LinearLayout.LayoutParams(0, 50, 1f).apply { leftMargin = 5; topMargin = 8 })
        root.addView(row)
        root.addView(button("✉  COMPOSE EMAIL") { composeEmail() }, LinearLayout.LayoutParams(-1, 48).apply { topMargin = 8 })
        setContentView(root)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(13, 65, 92))
        textSize = 13f; setOnClickListener { action() }
    }

    private fun status(message: String) { runOnUiThread { transcript.text = message } }

    private fun ask(text: String) {
        val question = text.trim()
        if (question.isEmpty()) { status("Type a message first."); return }
        val key = apiKey.text.toString().trim().ifEmpty { getPreferences(0).getString("gemini_key", "").orEmpty() }
        if (key.isBlank()) { status("Please enter your Gemini API key above and tap SAVE API KEY. Get a key at aistudio.google.com/apikey."); return }
        status("YOU\n$question\n\nJ.A.R.V.I.S.\nThinking...")
        worker.execute {
            try {
                val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + Uri.encode(key))
                val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", question)))))
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (code !in 200..299) throw Exception("Gemini HTTP $code: " + JSONObject(response).optJSONObject("error")?.optString("message", "Request failed"))
                val reply = JSONObject(response).getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                runOnUiThread {
                    transcript.text = "YOU\n$question\n\nJ.A.R.V.I.S.\n$reply"
                    if (ready) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
                }
            } catch (e: Exception) { status("Couldn't reach Gemini: " + (e.message ?: "unknown error")) }
        }
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to J.A.R.V.I.S.")
        }
        try { startActivityForResult(intent, 101) } catch (_: Exception) { status("Speech recognition isn't available on this device.") }
    }

    @Deprecated("Uses activity result for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) ask(words)
        }
    }

    private fun showContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 102); return
        }
        try {
            val intent = Intent(Intent.ACTION_PICK, android.provider.ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(intent, 103)
        } catch (_: Exception) { status("Couldn't open contacts.") }
    }

    private fun composeEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "Message from J.A.R.V.I.S.")
        }
        try { startActivity(Intent.createChooser(intent, "Choose email app")) }
        catch (_: Exception) { status("No email app is installed.") }
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.getDefault()
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        worker.shutdownNow()
        super.onDestroy()
    }
}
