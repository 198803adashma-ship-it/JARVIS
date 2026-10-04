package com.jarvis.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
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
    private TextView status;
    private TextView orb;
    private EditText input;

    private TextToSpeech tts;
    private SharedPreferences settings;

    private boolean ttsReady = false;
    private boolean speaking = false;

    private static final int VOICE_REQUEST = 100;
    private static final int MIC_PERMISSION = 101;

    private static final String GROQ_ENDPOINT =
            "https://api.groq.com/openai/v1/chat/completions";

    private static final String GROQ_MODEL =
            "openai/gpt-oss-20b";

    private final int cyan = Color.rgb(0, 220, 255);
    private final int dark = Color.rgb(3, 8, 15);
    private final int green = Color.rgb(0, 255, 150);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getSharedPreferences(
                "jarvis_settings",
                MODE_PRIVATE
        );

        buildInterface();
        initVoice();

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

    private void initVoice() {

        tts = new TextToSpeech(
                this,
                result -> {

                    if (result != TextToSpeech.SUCCESS) {

                        ttsReady = false;

                        runOnUiThread(() ->
                                setStatus("● VOICE ERROR")
                        );

                        return;
                    }

                    boolean ready = false;

                    // O'zbek tili
                    try {

                        int resultUz =
                                tts.setLanguage(
                                        new Locale("uz", "UZ")
                                );

                        if (resultUz !=
                                TextToSpeech.LANG_MISSING_DATA
                                &&
                                resultUz !=
                                TextToSpeech.LANG_NOT_SUPPORTED) {

                            ready = true;
                        }

                    } catch (Exception ignored) {
                    }

                    // O'zbek tili topilmasa,
                    // telefondagi mavjud ovozni izlaymiz
                    if (!ready) {

                        try {

                            for (Voice voice :
                                    tts.getVoices()) {

                                if (voice == null)
                                    continue;

                                Locale locale =
                                        voice.getLocale();

                                if (locale == null)
                                    continue;

                                if (!voice
                                        .isNetworkConnectionRequired()) {

                                    tts.setVoice(voice);
                                    ready = true;
                                    break;
                                }
                            }

                        } catch (Exception ignored) {
                        }
                    }

                    // Oxirgi fallback
                    if (!ready) {

                        try {

                            int resultEn =
                                    tts.setLanguage(
                                            Locale.US
                                    );

                            if (resultEn !=
                                    TextToSpeech.LANG_MISSING_DATA
                                    &&
                                    resultEn !=
                                    TextToSpeech.LANG_NOT_SUPPORTED) {

                                ready = true;
                            }

                        } catch (Exception ignored) {
                        }
                    }

                    try {
                        tts.setSpeechRate(0.88f);
                        tts.setPitch(1.03f);
                    } catch (Exception ignored) {
                    }

                    ttsReady = ready;

                    if (ready) {

                        runOnUiThread(() -> {

                            setStatus(
                                    "● ONLINE  |  VOICE READY"
                            );

                            speak(
                                    "Assalomu alaykum. " +
                                    "Men Jarvisman."
                            );
                        });

                    } else {

                        runOnUiThread(() ->
                                setStatus(
                                        "● VOICE NOT AVAILABLE"
                                )
                        );
                    }
                }
        );
    }

    private void speak(String text) {

        if (tts == null || !ttsReady) {
            return;
        }

        try {

            String cleanText =
                    cleanForSpeech(text);

            if (cleanText.isEmpty())
                return;

            speaking = true;

            runOnUiThread(() ->
                    setStatus("● SPEAKING...")
            );

            animateOrb(true);

            tts.speak(
                    cleanText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis_voice"
            );

        } catch (Exception ignored) {
        }
    }

    private String cleanForSpeech(String text) {

        if (text == null)
            return "";

        return text
                .replace("*", "")
                .replace("#", "")
                .replace("`", "")
                .replace("_", "")
                .replace("•", "")
                .replace("—", " ")
                .replace("–", " ")
                .replace("\n\n", ". ")
                .replace("\n", ". ")
                .trim();
    }

    private GradientDrawable background(
            int color,
            int radius,
            int strokeColor
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(
                2,
                strokeColor
        );

        return drawable;
    }

    private void buildInterface() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                22,
                25,
                22,
                18
        );

        root.setBackgroundColor(dark);

        // TITLE
        TextView title =
                new TextView(this);

        title.setText(
                "J A R V I S"
        );

        title.setTextColor(cyan);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.18f);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );

        // STATUS
        status =
                new TextView(this);

        status.setText(
                "● STARTING..."
        );

        status.setTextColor(green);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);

        root.addView(
                status,
                new LinearLayout.LayoutParams(
                        -1,
                        35
                )
        );

        // ORB
        orb =
                new TextView(this);

        orb.setText("◉");
        orb.setTextColor(cyan);
        orb.setTextSize(78);
        orb.setGravity(Gravity.CENTER);

        orb.setBackground(
                background(
                        Color.rgb(5, 20, 30),
                        300,
                        cyan
                )
        );

        LinearLayout.LayoutParams orbParams =
                new LinearLayout.LayoutParams(
                        190,
                        190
                );

        orbParams.gravity =
                Gravity.CENTER;

        orbParams.setMargins(
                0,
                10,
                0,
                15
        );

        root.addView(
                orb,
                orbParams
        );

        // READY TEXT
        TextView ready =
                new TextView(this);

        ready.setText(
                "JARVIS SYSTEM READY"
        );

        ready.setTextColor(cyan);
        ready.setTextSize(14);
        ready.setGravity(Gravity.CENTER);
        ready.setLetterSpacing(0.12f);

        root.addView(
                ready,
                new LinearLayout.LayoutParams(
                        -1,
                        32
                )
        );

        // CHAT
        ScrollView scroll =
                new ScrollView(this);

        chat =
                new TextView(this);

        chat.setText(
                "JARVIS: Assalomu alaykum.\n" +
                "Men ishga tayyorman.\n\n"
        );

        chat.setTextColor(
                Color.rgb(
                        220,
                        245,
                        255
                )
        );

        chat.setTextSize(17);
        chat.setPadding(
                15,
                15,
                15,
                15
        );

        chat.setBackground(
                background(
                        Color.rgb(
                                5,
                                12,
                                20
                        ),
                        25,
                        Color.rgb(
                                0,
                                90,
                                120
                        )
                )
        );

        scroll.addView(chat);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        // INPUT
        input =
                new EditText(this);

        input.setHint(
                "JARVISga buyruq bering..."
        );

        input.setHintTextColor(
                Color.rgb(
                        100,
                        140,
                        155
                )
        );

        input.setTextColor(Color.WHITE);
        input.setTextSize(16);
        input.setSingleLine(true);

        input.setPadding(
                20,
                5,
                20,
                5
        );

        input.setBackground(
                background(
                        Color.rgb(
                                8,
                                18,
                                28
                        ),
                        40,
                        Color.rgb(
                                0,
                                150,
                                190
                        )
                )
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        -1,
                        58
                );

        inputParams.setMargins(
                0,
                10,
                0,
                10
        );

        root.addView(
                input,
                inputParams
        );

        // BUTTONS
        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button send =
                createButton("YUBORISH");

        Button voice =
                createButton("🎙 OVOZ");

        Button settingsButton =
                createButton("⚙");

        settingsButton.setTextSize(20);

        buttons.addView(
                send,
                new LinearLayout.LayoutParams(
                        0,
                        58,
                        1
                )
        );

        buttons.addView(
                voice,
                new LinearLayout.LayoutParams(
                        0,
                        58,
                        1
                )
        );

        buttons.addView(
                settingsButton,
                new LinearLayout.LayoutParams(
                        70,
                        58
                )
        );

        root.addView(buttons);

        setContentView(root);

        send.setOnClickListener(
                v -> sendMessage()
        );

        voice.setOnClickListener(
                v -> startVoice()
        );

        settingsButton.setOnClickListener(
                v -> openSettings()
        );
    }

    private Button createButton(String text) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextColor(cyan);
        button.setTextSize(12);

        button.setBackground(
                background(
                        Color.rgb(
                                5,
                                25,
                                35
                        ),
                        35,
                        cyan
                )
        );

        return button;
    }

    private void sendMessage() {

        String question =
                input.getText()
                        .toString()
                        .trim();

        if (question.isEmpty())
            return;

        addMessage(
                "\nSIZ:\n" +
                question +
                "\n"
        );

        input.setText("");

        String apiKey =
                settings.getString(
                        "api_key",
                        ""
                );

        if (apiKey.isEmpty()) {

            String answer =
                    "Groq API kaliti kiritilmagan.";

            addMessage(
                    "\nJARVIS:\n" +
                    answer +
                    "\n"
            );

            speak(answer);
            return;
        }

        setStatus("● THINKING...");
        animateOrb(false);

        new Thread(() -> {

            String answer;

            try {

                answer =
                        askGroq(
                                question,
                                apiKey
                        );

            } catch (Exception e) {

                answer =
                        "Xatolik yuz berdi. " +
                        e.getMessage();
            }

            String finalAnswer =
                    answer;

            runOnUiThread(() -> {

                setStatus(
                        "● ONLINE  |  VOICE READY"
                );

                addMessage(
                        "\nJARVIS:\n" +
                        finalAnswer +
                        "\n"
                );

                speak(finalAnswer);
            });

        }).start();
    }

    private String askGroq(
            String question,
            String apiKey
    ) throws Exception {

        URL url =
                new URL(GROQ_ENDPOINT);

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setRequestMethod(
                "POST"
        );

        connection.setRequestProperty(
                "Authorization",
                "Bearer " + apiKey
        );

        connection.setRequestProperty(
                "Content-Type",
                "application/json"
        );

        connection.setConnectTimeout(
                20000
        );

        connection.setReadTimeout(
                60000
        );

        connection.setDoOutput(true);

        JSONObject system =
                new JSONObject();

        system.put(
                "role",
                "system"
        );

        system.put(
                "content",
                "Sen JARVIS nomli aqlli " +
                "shaxsiy yordamchisan. " +
                "Faqat tabiiy va ravon " +
                "o'zbek tilida javob ber. " +
                "Javobni qisqa, aniq va " +
                "insondek tabiiy qil. " +
                "Ovoz chiqarib o'qilganda " +
                "qulay eshitilsin. " +
                "Emoji ishlatma. " +
                "Markdown, yulduzcha, " +
                "xeshteg va murakkab " +
                "format ishlatma."
        );

        JSONObject user =
                new JSONObject();

        user.put(
                "role",
                "user"
        );

        user.put(
                "content",
                question
        );

        JSONArray messages =
                new JSONArray();

        messages.put(system);
        messages.put(user);

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

        if (code >= 200 &&
                code < 300) {

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection
                                            .getInputStream()
                            )
                    );

        } else {

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection
                                            .getErrorStream()
                            )
                    );
        }

        StringBuilder response =
                new StringBuilder();

        String line;

        while (
                (line =
                        reader.readLine())
                        != null
        ) {

            response.append(line);
        }

        reader.close();

        if (code < 200 ||
                code >= 300) {

            return
                    "Groq xatosi " +
                    code +
                    ".";
        }

        JSONObject result =
                new JSONObject(
                        response.toString()
                );

        return result
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    private void startVoice() {

        setStatus(
                "● LISTENING..."
        );

        animateOrb(false);

        Intent intent =
                new Intent(
                        RecognizerIntent
                                .ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent
                        .EXTRA_LANGUAGE,
                "uz-UZ"
        );

        intent.putExtra(
                RecognizerIntent
                        .EXTRA_LANGUAGE_MODEL,
                RecognizerIntent
                        .LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent
                        .EXTRA_PROMPT,
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
                    "Ovozli xizmat mavjud emas",
                    Toast.LENGTH_SHORT
            ).show();

            setStatus(
                    "● ONLINE"
            );
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

        if (
                requestCode ==
                        VOICE_REQUEST
                        &&
                resultCode ==
                        RESULT_OK
                        &&
                data != null
        ) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent
                                    .EXTRA_RESULTS
                    );

            if (
                    results != null
                    &&
                    !results.isEmpty()
            ) {

                input.setText(
                        results.get(0)
                );

                sendMessage();

            } else {

                setStatus(
                        "● ONLINE"
                );
            }
        }
    }

    private void setStatus(
            String text
    ) {

        if (status != null) {

            status.setText(text);
        }
    }

    private void animateOrb(
            boolean active
    ) {

        if (orb == null)
            return;

        if (active) {

            AlphaAnimation animation =
                    new AlphaAnimation(
                            0.35f,
                            1.0f
                    );

            animation.setDuration(500);
            animation.setRepeatMode(
                    AlphaAnimation.REVERSE
            );

            animation.setRepeatCount(
                    AlphaAnimation.INFINITE
            );

            orb.startAnimation(animation);

        } else {

            orb.clearAnimation();
            orb.setAlpha(1.0f);
        }
    }

    private void addMessage(
            String message
    ) {

        chat.append(message);

        chat.post(() -> {

            View parent =
                    (View) chat.getParent();

            if (parent instanceof
                    ScrollView) {

                ((ScrollView) parent)
                        .fullScroll(
                                View.FOCUS_DOWN
                        );
            }
        });
    }

    private void openSettings() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                30,
                40,
                30,
                30
        );

        layout.setBackgroundColor(dark);

        TextView title =
                new TextView(this);

        title.setText(
                "JARVIS SETTINGS"
        );

        title.setTextColor(cyan);
        title.setTextSize(25);
        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                0,
                0,
                25
        );

        EditText key =
                new EditText(this);

        key.setHint(
                "Groq API Key"
        );

        key.setTextColor(
                Color.WHITE
        );

        key.setHintTextColor(
                Color.GRAY
        );

        key.setSingleLine(true);

        key.setText(
                settings.getString(
                        "api_key",
                        ""
                )
        );

        Button save =
                new Button(this);

        save.setText(
                "SAQLASH"
        );

        save.setTextColor(cyan);

        layout.addView(title);
        layout.addView(key);
        layout.addView(save);

        setContentView(layout);

        save.setOnClickListener(v -> {

            settings.edit()
                    .putString(
                            "api_key",
                            key.getText()
                                    .toString()
                                    .trim()
                    )
                    .apply();

            Toast.makeText(
                    this,
                    "Sozlamalar saqlandi",
                    Toast.LENGTH_SHORT
            ).show();

            buildInterface();
            initVoice();
        });
    }

    @Override
    protected void onDestroy() {

        if (tts != null) {

            try {

                tts.stop();
                tts.shutdown();

            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
}
