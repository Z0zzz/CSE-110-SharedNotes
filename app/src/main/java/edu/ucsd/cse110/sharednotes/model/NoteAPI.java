package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NoteAPI {
    // TODO: Implement the API using OkHttp!
    // TODO: - getNote (maybe getNoteAsync)
    // TODO: - putNote (don't need putNotAsync, probably)
    // TODO: Read the docs: https://square.github.io/okhttp/
    // TODO: Read the docs: https://sharednotes.goto.ucsd.edu/docs

    private volatile static NoteAPI instance = null;
    public static final MediaType JSON
            = MediaType.parse("application/json");

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Note get(String title){

        // Log.i("Trying response", "something?");

        title = title.replace(" ", "%20");
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            String jsonObject = response.body().string();
            // Log.i("Trying response", "something?" + jsonObject);
            Note note = Note.fromJSON(jsonObject);
            Log.i("get", "note content " + note.content);
            return note;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public String put(Note note){
        Log.i("put", ""+note.content);
        String title = note.title.replace(" ", "%20");
        String jsonNote = note.toJSON();
        Gson gson = new Gson();
        RequestBody body = RequestBody.create(JSON, gson.toJson(Map.of( "content", note.content, "version", note.version)));
        Log.i("put", "requestbody "+body);
        Request request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .put(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            Log.i("put", gson.toJson(Map.of( "content", note.content, "version", note.version)));
            return response.body().string();
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public Future<String> putAsync(Note note) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> put(note));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }



    @AnyThread
    public Future<Note> getAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> get(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }


    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> echo(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }
}
