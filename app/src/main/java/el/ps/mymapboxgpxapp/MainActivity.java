package el.ps.mymapboxgpxapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    Button btnCall;
    private Dialog loadingDialog;
    private Handler handler = new Handler(Looper.getMainLooper());
    String nngUrl = "http://nextetruck.nng.com:5001/?ApiKey=WsJhIClUoGhV1ohCNFnAxk5tJT7hAd6X3qkIhbgBNOMMXIpgsgd1WUoFKahck8wW"; //FULL SERVER
    private final String vehicle_id = "Certh_Vehicle1";
    private String m_departure_lat = "40.567673";
    private String m_departure_long = "22.996658";
    private String m_arrival_lat = "40.549737";
    private String m_arrival_long = "22.2649827";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCall = findViewById(R.id.btnGPX);
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SynchronousHttpRequests().executeRequests();
            }
        });
    }

    private class SynchronousHttpRequests {

        // Main executor for background tasks
        private ExecutorService executorService = Executors.newSingleThreadExecutor();

        public void executeRequests() {
            showLoadingDialog();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // First HTTP request
                        //DYNAMIC
                        String response1 = makeHttpRequest(nngUrl+"&SetPathFormat=GPX&Vehicle="+vehicle_id);

                        Log.d("HTTP Response 1", response1);

                        if(response1.equals("Request accepted, but processing not complete."))
                        {
                            // Second HTTP request
                            String response2 = makeHttpRequest(nngUrl+"&TargetCoordinate="+m_arrival_lat+","+m_arrival_long+"&Vehicle="+vehicle_id);
                            Log.d("HTTP Response 2", response2);

                            if(response2.equals("Request accepted, but processing not complete."))
                            {
                                //DYNAMIC
                                InputStream inputStream = new ByteArrayInputStream(makeHttpRequest(nngUrl+"&SourceCoordinate="+m_departure_lat+","+m_departure_long+":3.51&Vehicle="+vehicle_id).getBytes());
                                // Save json on global variables
                                GlobalData globalData = GlobalData.getInstance();
                                globalData.setGlobalXMLObject(inputStream);

                                // Load Map Screen
                                Intent i = new Intent(MainActivity.this, Page2Map.class);
                                startActivity(i);
                            }
                        }

                        // Post results to the main thread if necessary
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                dismissLoadingDialog();
                                // Update UI with results here if needed
                            }
                        });
                    } catch (Exception e) {
                        Log.e("SynchronousHttpRequests", "Error during HTTP requests", e);
                        e.printStackTrace();
                    }
                }
            });
        }

        private String makeHttpRequest(String urlString) throws Exception {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setRequestMethod("GET");
                int responseCode = urlConnection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } else {
                    Log.w("SynchronousHttpRequests", "Non-OK response: " + responseCode + " for URL: " + urlString);
                    if (responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                        // Handle 202 Accepted specifically if needed
                        return "Request accepted, but processing not complete.";
                    } else {
                        throw new Exception("Failed to make request: " + responseCode);
                    }
                }
            }
            catch (Exception e) {
                Log.e("SynchronousHttpRequests", "Error in makeHttpRequest: " + urlString, e);
                throw e;
            }
            finally {
                urlConnection.disconnect();
            }
        }
    }

    private void showLoadingDialog() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                loadingDialog = new Dialog(MainActivity.this);
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_loading, null);
                loadingDialog.setContentView(view);
                loadingDialog.setCancelable(false);
                loadingDialog.show();
            }
        });
    }

    private void dismissLoadingDialog() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            }
        });
    }
}