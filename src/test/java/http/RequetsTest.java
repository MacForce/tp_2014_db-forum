package http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RequetsTest {
    private final String USER_AGENT = "Chrome/10.0.648.205";

    public static void main(String[] args) throws Exception {
        RequetsTest http = new RequetsTest();
//        for (final AtomicInteger i = new AtomicInteger(0); i.get() < 40;) {
//            Thread thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        http.sendPost(i.incrementAndGet());
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            thread.start();
//        }
        http.sendPost();
//        http.sendGet();
    }

    // HTTP GET request
    private void sendGet() throws Exception {

        String url = "http://localhost:8080/db/api/forum/listPosts/?related=thread&related=forum&since=2014-01-01+00%3A00%3A00&order=desc&forum=forum1";
//        String url = "http://localhost:8080/db/api/user/listPosts/?since=2014-01-02+00%3A00%3A00&limit=2&user=example%40mail.ru&order=asc";

        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(url);

        HttpResponse response = client.execute(get);
        System.out.println("Sending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + response.toString());

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        System.out.println("Response body : " + result.toString());
    }

    // HTTP POST request
    private void sendPost() throws Exception {

        String url = "http://localhost:8080/db/api/user/follow/";

        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.setHeader("User-Agent", USER_AGENT);

//        List<NameValuePair> urlParameters = new ArrayList<>();
//        urlParameters.add(new BasicNameValuePair("user", "example@mail.ru"));
//        post.setEntity(new UrlEncodedFormEntity(urlParameters, "UTF-8"));

//        StringEntity input = new StringEntity("{\"username\": \"user1\", \"about\": \"hello im user1\", " +
//                "\"isAnonymous\": false, \"name\": \"John\", \"email\": \"example123@mail.ru\"}");

        StringEntity input = new StringEntity("{\"follower\": \"example3@mail.ru\", \"followee\": \"example@mail.ru\"}");
        input.setContentType("application/json");
        post.setEntity(input);

        HttpResponse response = client.execute(post);
        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + post.getEntity());
        System.out.println("Response Code : " + response.toString());

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        System.out.println("Response body : " + result.toString());
    }
}
