package com.bohn.teachme2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.io.Serial;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {

    private String url = "http://192.168.50.176:1550";
    private TextView output;
    private EditText input;

    private static final MediaType JSON = MediaType.get("application/json");
    private OkHttpClient client;

    private ArrayList<String> context = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        output = findViewById(R.id.textViewAI);
        input = findViewById(R.id.editTextUserAIInput);

        String result = "XXX";
        Log.d("CODE", "Hello World");
    }

    public void buttonPressed(View view) {
        String question = input.getText().toString();
        promptServer(question, url);
//        Thread th = new Thread(new Runnable() {
//            String demo = "";
//
//            @Override
//            public void run() {
//                try {
//                    demo = requestPlease("https://github.com/strangejmaster/Retention-Trainer/");
//                    Log.d("CODE", "result");
////            throw new IOException("test");
//                } catch (IOException e) {
//                    Log.d("CODE", "Fatal Error: " + e.getMessage());
//                }
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        output.setText(demo);
//                    }
//                });
//            }
//        });
//        th.start();
    }

    public void promptServer(String question, String url) {

        Thread backProcess = new Thread(new Runnable() {
            String result = "XXX";
            String content = "";
            @Override
            public void run() {
                String endpoint = "/v1/chat/completions";

                Log.d("CODE",  "URI/URL: " + url + endpoint);

                String call = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>";

                call = "{" +
                        "\"model\": \"meta-llama-3.1-8b-instruct\"," +
                        "\"messages\": [" +
                        "{" +
                        "\"role\": \"developer\"," +
                        "\"content\": \"You are an AI that behaves like a curious student. You only know the minimum required knowledge not information on the specific topic within the subject that the teacher is teaching. You only learn from the information provided in the user’s prompt. If something is not explained in the prompt, you treat it as unknown and do not guess.\"" +
                        "},{" +
                        "\"role\": \"user\"," +
                        "\"content\": \"" + question + "\"" +
//                        "\"temperature\": 0.7," +
//                        "\"max_tokens\": 200," +
//                        "\"stream\": false," +
//                        "\"stop\": \"\n\"" +
                        "}]}";

                Log.d("CODE", "MESSAGE: " + call);

                RequestBody requestBody = RequestBody.create(call, JSON);

                Request req = new Request.Builder()
                        .url(url + endpoint)
                        .post(requestBody)
                        .build();

                try {
                    Response res = client.newCall(req).execute();
                    result = res.body().string();
                } catch (IOException e) {
                    result = "Error calling the API: " + e.getMessage();
                }

//                Parsing Output
                Log.d("CODE", "Content: " + result);
                try {
                    JSONObject obj = new JSONObject(result);
                    content = obj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").get("content").toString();
                } catch (Exception e) {
                    content = "Error: " + e.getMessage();
                }
                Log.d("CODE", "Content: "+ content);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        output.setText(output.getText().toString() + "\n" + content);
                    }
                });
            }
        });
        backProcess.start();
    }
}