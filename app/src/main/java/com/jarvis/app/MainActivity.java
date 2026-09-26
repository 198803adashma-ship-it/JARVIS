package com.jarvis.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView chat;
    private EditText input;
    private TextToSpeech tts;
    private SharedPreferences settings;

    private static final int VOICE_REQUEST = 100;
    private static final int MIC_PERMISSION = 101;

    private static final String GROQ_ENDPOINT =
            "https://api.groq.com/openai/v1/chat/completions";

    private static final String GROQ_MODEL =
            "openai/gpt-oss-20b";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getSharedPreferences(
                "jarvis_settings",
                MODE_PRIVATE
        );

        buildInterface();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(
                        new Locale("uz", "UZ")
                );

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    tts.setLanguage(Locale.US);
                }
            }
        });

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    MIC_PERMISSION
            );
        }
    }

    private void buildInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 30, 24, 20);

        TextView title = new TextView(this);
        title.setText("🤖 JARVIS");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 25);

        root.addView(title);

        ScrollView scroll = new ScrollView(this);

        chat = new TextView(this);

        chat.setText(
                "JARVIS tayyor.\n\n" +
                "Assalomu alaykum! Men sizning AI yordamchingizman.\n\n"
        );

        chat.setTextSize(18);
        chat.setPadding(10, 10, 10, 10);

        scroll.addView(chat);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        input = new EditText(this);
        input.setHint("JARVISga savol yozing...");
        input.setTextSize(17);

        root.addView(input);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button send = new Button(this);
        send.setText("Yuborish");

        Button voice = new Button(this);
        voice.setText("🎙 Ovoz");

        Button settingsButton = new Button(this);
        settingsButton.setText("⚙️");

        buttons.addView(
                send,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        buttons.addView(
                voice,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        buttons.addView(settingsButton);

        root.addView(buttons);

        setContentView(root);

        send.setOnClickListener(v -> sendMessage());

        voice.setOnClickListener(v -> startVoice());

        settingsButton.setOnClickListener(
                v -> openSettings()
        );
    }

    private void sendMessage() {

        String question =
                input.getText().toString().trim();

        if (question.isEmpty()) {
            return;
        }

        addMessage("Siz: " + question);

        input.setText("");

        String apiKey =
                settings.getString("api_key", "");

        if (apiKey.isEmpty()) {

            String answer =
                    "Groq API kaliti hali kiritilmagan.\n\n" +
                    "⚙️ tugmasini bosib Groq API kalitini kiriting.";

            addMessage("JARVIS: " + answer);
            speak(answer);

            return;
        }

        addMessage("🧠 Groq AI ishlayapti...");

        new Thread(() -> {

            String answer;

            try {

                answer = askGroq(
                        question,
                        apiKey
                );

            } catch (Exception e) {

                answer =
                        "Groq bilan ulanishda xatolik:\n\n" +
                        e.getMessage();
            }

            final String finalAnswer = answer;

            runOnUiThread(() -> {

                addMessage(
                        "JARVIS: " + finalAnswer
                );

                speak(finalAnswer);
            });

        }).start();
    }

    private String askGroq(
            String question,
            String apiKey
    ) throws Exception {

        URL url = new URL(GROQ_ENDPOINT);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");

        connection.setRequestProperty(
                "Authorization",
                "Bearer " + apiKey
        );

        connection.setRequestProperty(
                "Content-Type",
                "application/json"
        );

        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);

        connection.setDoOutput(true);

        JSONObject systemMessage =
                new JSONObject();

        systemMessage.put(
                "role",
                "system"
        );

        systemMessage.put(
                "content",
                "You are JARVIS, a helpful AI assistant. " +
                "Always answer the user in Uzbek when possible. " +
                "Be concise, friendly and useful."
        );

        JSONObject userMessage =
                new JSONObject();

        userMessage.put(
                "role",
                "user"
        );

        userMessage.put(
                "content",
                question
        );

        JSONArray messages =
                new JSONArray();

        messages.put(systemMessage);
        messages.put(userMessage);

        JSONObject body =
                new JSONObject();

        body.put(
                "model",
                GROQ_MODEL
        );

        body.put(
                "messages",
                messages
        );

        body.put(
                "temperature",
                0.7
        );

        OutputStream output =
                connection.getOutputStream();

        output.write(
                body.toString()
                        .getBytes("UTF-8")
        );

        output.flush();
        output.close();

        int code =
                connection.getResponseCode();

        BufferedReader reader;

        if (code >= 200 && code < 300) {

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

        } else {

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getErrorStream()
                            )
                    );
        }

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        if (code < 200 || code >= 300) {

            return "Groq HTTP xatosi: " +
                    code +
                    "\n\n" +
                    response;
        }

        JSONObject result =
                new JSONObject(
                        response.toString()
                );

        JSONArray choices =
                result.getJSONArray("choices");

        JSONObject first =
                choices.getJSONObject(0);

        JSONObject message =
                first.getJSONObject("message");

        return message.getString("content");
    }

    private void startVoice() {

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "uz-UZ"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "JARVIS tinglamoqda..."
        );

        try {

            startActivityForResult(
                    intent,
                    VOICE_REQUEST
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Ovozli qidiruv mavjud emas",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == VOICE_REQUEST &&
                resultCode == RESULT_OK &&
                data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null &&
                    !results.isEmpty()) {

                input.setText(
                        results.get(0)
                );

                sendMessage();
            }
        }
    }

    private void addMessage(String message) {

        chat.append(
                "\n" + message + "\n"
        );
    }

    private void speak(String text) {

        if (tts != null) {

            tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis"
            );
        }
    }

    private void openSettings() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                30,
                30,
                30,
                30
        );

        TextView title =
                new TextView(this);

        title.setText(
                "JARVIS — Groq sozlamalari"
        );

        title.setTextSize(22);

        EditText key =
                new EditText(this);

        key.setHint(
                "Groq API Key"
        );

        key.setText(
                settings.getString(
                        "api_key",
                        ""
                )
        );

        key.setSingleLine(true);

        EditText endpoint =
                new EditText(this);

        endpoint.setHint(
                "API Endpoint"
        );

        endpoint.setText(
                GROQ_ENDPOINT
        );

        endpoint.setSingleLine(true);

        EditText model =
                new EditText(this);

        model.setHint(
                "Model"
        );

        model.setText(
                GROQ_MODEL
        );

        model.setSingleLine(true);

        Button save =
                new Button(this);

        save.setText(
                "Saqlash"
        );

        layout.addView(title);
        layout.addView(key);
        layout.addView(endpoint);
        layout.addView(model);
        layout.addView(save);

        setContentView(layout);

        save.setOnClickListener(v -> {

            settings.edit()
                    .putString(
                            "api_key",
                            key.getText().toString().trim()
                    )
                    .apply();

            Toast.makeText(
                    this,
                    "Groq API saqlandi",
                    Toast.LENGTH_SHORT
            ).show();

            buildInterface();
        });
    }

    @Override
    protected void onDestroy() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
