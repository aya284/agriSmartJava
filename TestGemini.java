import java.io.*;
import java.net.*;
public class TestGemini {
    public static void main(String[] args) throws Exception {
        String key = "AIzaSyBHCnpS6wEptcgRgTNezIJ4kDP0cNPTfAw";
        String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + key;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String payload = "{\"contents\":[{\"parts\":[{\"text\":\"test\"}]}]}";
        try(OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes("UTF-8"));
        }
        int code = conn.getResponseCode();
        System.out.println("Code: " + code);
        if(code != 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            String line;
            while((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
