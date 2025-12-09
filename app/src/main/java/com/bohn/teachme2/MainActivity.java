package com.bohn.teachme2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    // UI Components
    private EditText editTextUserIPInput;
    private TextView textViewAI;
    private EditText editTextUserAIInput;
    private Button buttonSubmitInput;
    private Spinner spinnerModels; // Added for model selection

    private Handler uiHandler;

    // Data Structures
    private List<Message> chatHistory = new ArrayList<>();

    // --- Data Class for Chat History ---
    private static class Message {
        final String role; // "system", "user", or "assistant"
        final String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // Method to convert this message to a JSON object for the API
        JSONObject toJsonObject() {
            try {
                JSONObject json = new JSONObject();
                json.put("role", role);
                json.put("content", content);
                return json;
            } catch (Exception e) {
                Log.e(TAG, "Error creating message JSON", e);
                return new JSONObject();
            }
        }
    }

    // --- Lifecycle Methods ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        editTextUserIPInput = findViewById(R.id.editTextUserIPInput);
        textViewAI = findViewById(R.id.textViewAI);
        editTextUserAIInput = findViewById(R.id.editTextUserAIInput);
        buttonSubmitInput = findViewById(R.id.buttonSubmitInput);
        spinnerModels = findViewById(R.id.spinnerModels); // Make sure to add this to your XML

        uiHandler = new Handler(Looper.getMainLooper());

        // Example IP/Port (Set a default for quick testing)
        editTextUserIPInput.setText("192.168.0.71:1234");

        // Initialize history with a system message
        chatHistory.add(new Message("system", "You are a helpful and friendly AI assistant. Keep your responses concise."));

        // Start the process by fetching available models
        fetchModels();

        Log.d(TAG, "MainActivity created.");
    }

    // --- Model Fetching Logic (GET /v1/models) ---

    private void fetchModels() {
        String serverAddress = editTextUserIPInput.getText().toString();
        if (serverAddress.isEmpty()) {
            Toast.makeText(this, "Please enter Server IP:Port.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start the model fetching in a new thread
        new Thread(new ModelFetchRunnable(serverAddress)).start();
    }

    // Method to display models in the Spinner
    private void populateModelSpinner(List<String> modelIds) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                modelIds);
        spinnerModels.setAdapter(adapter);
    }

    // --- Button Click Handler (Chat Submission) ---

    public void buttonPressed(View view) {
        String serverAddress = editTextUserIPInput.getText().toString();
        String userMessage = editTextUserAIInput.getText().toString().trim();
        String selectedModel = (String) spinnerModels.getSelectedItem();

        if (serverAddress.isEmpty() || userMessage.isEmpty() || selectedModel == null) {
            Toast.makeText(this, "Please ensure IP, Message, and Model are selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Add the user's message to the history
        Message userMsg = new Message("user", userMessage);
        chatHistory.add(userMsg);

        // 2. Clear input and update UI with current history
        editTextUserAIInput.setText("");
        updateChatUI();

        // 3. Start the HTTP request in a new thread
        new Thread(new HttpClientRunnable(serverAddress, selectedModel)).start();
        Log.i(TAG, "Starting chat request for model: " + selectedModel);
    }

    // --- UI Update Method ---

    private void updateChatUI() {
        StringBuilder sb = new StringBuilder();
        for (Message msg : chatHistory) {
            // Skip the system message for display
            if (!"system".equals(msg.role)) {
                sb.append("**").append(msg.role.toUpperCase()).append(":**\n")
                        .append(msg.content).append("\n\n");
            }
        }
        // Use a single setText call to minimize UI redrawing
        textViewAI.setText(sb.toString());
    }


    // --- Model Fetching Runnable ---

    class ModelFetchRunnable implements Runnable {
        private final String serverAddress;
        private final OkHttpClient client = new OkHttpClient();

        public ModelFetchRunnable(String serverAddress) {
            this.serverAddress = serverAddress;
        }

        @Override
        public void run() {
            List<String> modelIds = new ArrayList<>();
            String urlString = "http://" + serverAddress + "/v1/models";

            Request request = new Request.Builder()
                    .url(urlString)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch models: " + response.code() + " " + response.message());
                }

                String responseString = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseString);

                JSONArray dataArray = jsonResponse.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject modelObject = dataArray.getJSONObject(i);
                    String modelId = modelObject.getString("id");
                    modelIds.add(modelId);
                }

            } catch (Exception e) {
                Log.e(TAG, "Model Fetch Error", e);
                uiHandler.post(() -> Toast.makeText(MainActivity.this, "Failed to connect or fetch models. Check IP/Port.", Toast.LENGTH_LONG).show());
            }

            uiHandler.post(() -> {
                if (!modelIds.isEmpty()) {
                    populateModelSpinner(modelIds);
                } else {
                    Toast.makeText(MainActivity.this, "No models found. Check LM Studio server status.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    // --- Chat Completion Runnable (POST /v1/chat/completions) ---

    class HttpClientRunnable implements Runnable {
        private final String serverAddress;
        private final String modelId;
        private final OkHttpClient client = new OkHttpClient();

        public HttpClientRunnable(String serverAddress, String modelId) {
            this.serverAddress = serverAddress;
            this.modelId = modelId;
        }

        @Override
        public void run() {
            String serverResponse = null;
            String endpoint = "/v1/chat/completions";

            try {
                // 1. Construct the base URL
                String urlString = "http://" + serverAddress + endpoint;

                // 2. Build the JSON request body
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", modelId);

                // Add the entire chat history
                JSONArray messagesArray = new JSONArray();
                for (Message msg : chatHistory) {
                    messagesArray.put(msg.toJsonObject());
                }
                jsonBody.put("messages", messagesArray);

                // Optional: Add generation parameters
                jsonBody.put("temperature", 0.7);
                jsonBody.put("max_tokens", 512);

                RequestBody body = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);

                // 3. Build the HTTP POST request
                Request request = new Request.Builder()
                        .url(urlString)
                        .post(body)
                        .build();

                Log.d(TAG, "Sending JSON: " + jsonBody.toString());

                // 4. Execute the request
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Server error code: " + response.code() + ", Message: " + response.message());
                    }

                    // 5. Parse the JSON response
                    String responseString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseString);

                    // Extract content from the LM Studio/OpenAI API structure
                    String aiContent = jsonResponse
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    serverResponse = aiContent;
                    Log.i(TAG, "AI Content received.");
                }

            } catch (Exception e) {
                Log.e(TAG, "HTTP Request Error", e);
                serverResponse = "ERROR: Connection or parsing failed. Details: " + e.getMessage();
            }

            // Update UI on the main thread
            final String finalResponse = serverResponse;
            uiHandler.post(() -> {
                if (finalResponse.startsWith("ERROR")) {
                    Toast.makeText(MainActivity.this, "Request Failed.", Toast.LENGTH_LONG).show();
                    // If an error occurred, remove the last user message from history
                    if (!chatHistory.isEmpty()) {
                        chatHistory.remove(chatHistory.size() - 1);
                    }
                } else {
                    // 6. Add the AI's successful response to the chat history
                    chatHistory.add(new Message("assistant", finalResponse));
                    Toast.makeText(MainActivity.this, "Response received.", Toast.LENGTH_SHORT).show();
                }

                // Update the UI with the full, final history
                updateChatUI();
            });
        }
    }
}