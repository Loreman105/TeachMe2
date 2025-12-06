package com.bohn.teachme2;

import android.os.Bundle;
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

import java.io.IOException;
import java.io.Serial;
import java.net.*;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {
    private String url = "http://192.168.152.1:1550";
    private TextView output;
    private EditText input;
//    private Button submit;

    private static final MediaType JSON = MediaType.get("application/json");
//    public OkHttpClient client;
OkHttpClient client = new OkHttpClient();

    String requestPlease(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
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

        output = findViewById(R.id.textViewAI);
        input = findViewById(R.id.editTextUserAIInput);

//        client = new OkHttpClient();
//
//        client = new OkHttpClient();
//        Request req = new Request.Builder()
//                .url("https://raw.github.com/square/okhttp/master/README.md")
//                .build();

        String result = "XXX";
        Log.d("CODE", "HelloWORLD");


//        try (Response res = client.newCall(req).execute()) {
//            result = res.body().string();
//        } catch (java.io.IOException | IllegalStateException e) {
//            output.setText("FAILURE: " + e.getMessage());
//        }
    }

    public void buttonPressed(View view) {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = requestPlease("https://github.com/strangejmaster/Retention-Trainer/");
                    Log.d("CODE", "result");
//            throw new IOException("test");
                } catch (IOException e) {
                    Log.d("CODE", "Fatal Error: " + e.getMessage());
                }
            }
        });
        th.start();
        String question = input.getText().toString();
//        try {
//            String result = promptServer(question, url);
////            output.setText(result);
//        } catch (IOException e) {
////            output.setText("There's been an error");
//        }

    }

    public String promptServer(String question, String url) throws IOException {
        String endpoint = "/api/v0/completions";

        String call = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>";

        call = "{" +
                "\"model\": \"meta-llama-3.1-8b-instruct\"," +
                "\"prompt\": \"<|begin_of_text|><|start_header_id|>system<|end_header_id|>\"," +
                "\"temperature\": 0.7," +
                "\"max_tokens\": 200," +
                "\"stream\": false," +
                "\"stop\": \"\n\"}";
        call = "{}";
        System.out.println("Call: ");
        System.out.print(call);

        RequestBody requestBody = RequestBody.create(call, JSON);

        Request req = new Request.Builder()
                .url("https://raw.github.com/square/okhttp/master/README.md")
                .build();

        String result = "XXX";

        try (Response res = client.newCall(req).execute()) {
            result = res.body().string();
        }
//        try {
//            Response res = client.newCall(req).execute();
//            result = res.body().string();
//        } catch (IOException e) {
//            result = "Error calling the API: " + e.getMessage();
//        }

        return result;
    }


}