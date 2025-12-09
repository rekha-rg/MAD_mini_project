package com.example.weatherupdate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private EditText etCity, etCountry;
    private Button btnGet;
    private TextView tvResult;

    // TODO: Replace with your OpenWeatherMap API key
    private static final String API_KEY = "YOUR_OPENWEATHERMAP_API_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCity = findViewById(R.id.etCity);
        etCountry = findViewById(R.id.etCountry);
        btnGet = findViewById(R.id.btnGet);
        tvResult = findViewById(R.id.tvResult);

        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = etCity.getText().toString().trim();
                String country = etCountry.getText().toString().trim();

                if (TextUtils.isEmpty(city)) {
                    Toast.makeText(MainActivity.this, "Enter City Name", Toast.LENGTH_SHORT).show();
                    return;
                }

                String query = city;
                if (!TextUtils.isEmpty(country)) {
                    query += "," + country;
                }

                try {
                    query = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String url = "https://api.openweathermap.org/data/2.5/weather?q=" + query + "&units=metric&appid=" + API_KEY;
                new FetchWeatherTask().execute(url);
            }
        });
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String urlString = strings[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null) return null;

                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append('\n');
                }

                return buffer.toString();

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                if (reader != null) {
                    try { reader.close(); } catch (IOException e) { e.printStackTrace(); }
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s == null) {
                tvResult.setText("Failed to fetch data. Check network or API key.");
                return;
            }

            try {
                JSONObject root = new JSONObject(s);

                String cityName = root.optString("name", "");
                JSONObject sys = root.optJSONObject("sys");
                String country = sys != null ? sys.optString("country", "") : "";

                JSONObject main = root.optJSONObject("main");
                double temp = main != null ? main.optDouble("temp", Double.NaN) : Double.NaN;
                double feels = main != null ? main.optDouble("feels_like", Double.NaN) : Double.NaN;
                int humidity = main != null ? main.optInt("humidity", -1) : -1;
                double pressure = main != null ? main.optDouble("pressure", Double.NaN) : Double.NaN;

                JSONArray weatherArr = root.optJSONArray("weather");
                String description = "";
                if (weatherArr != null && weatherArr.length() > 0) {
                    JSONObject w0 = weatherArr.getJSONObject(0);
                    description = w0.optString("description", "");
                }

                JSONObject wind = root.optJSONObject("wind");
                double windSpeed = wind != null ? wind.optDouble("speed", Double.NaN) : Double.NaN;

                JSONObject clouds = root.optJSONObject("clouds");
                int cloudiness = clouds != null ? clouds.optInt("all", -1) : -1;

                StringBuilder out = new StringBuilder();
                out.append("Current weather of ").append(cityName);
                if (!TextUtils.isEmpty(country)) out.append(" (").append(country).append(")");
                out.append('\n');
                if (!Double.isNaN(temp)) out.append("Temp: ").append(String.format("%.2f °C", temp)).append('\n');
                if (!Double.isNaN(feels)) out.append("Feels Like: ").append(String.format("%.2f °C", feels)).append('\n');
                if (humidity != -1) out.append("Humidity: ").append(humidity).append("%\n");
                if (!TextUtils.isEmpty(description)) out.append("Description: ").append(description).append('\n');
                if (!Double.isNaN(windSpeed)) out.append("Wind Speed: ").append(String.format("%.2fm/s (meters per second)", windSpeed)).append('\n');
                if (cloudiness != -1) out.append("Cloudiness: ").append(cloudiness).append("%\n");
                if (!Double.isNaN(pressure)) out.append("Pressure: ").append(String.format("%.1f hPa", pressure)).append('\n');

                tvResult.setText(out.toString());

            } catch (JSONException e) {
                e.printStackTrace();
                tvResult.setText("Parsing error: " + e.getMessage());
            }
        }
    }
}
