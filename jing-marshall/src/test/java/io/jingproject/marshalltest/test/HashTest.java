package io.jingproject.marshalltest.test;

import io.jingproject.marshall.HashUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HashTest {

    private static final int BATCH = 1000;

    private static HttpClient createHttpClient() {
        String proxyProperty = System.getProperty("http.proxy");
        if(proxyProperty == null) {
            return HttpClient.newHttpClient();
        } else {
            String[] splitStr = proxyProperty.split(":");
            if(splitStr.length != 2) {
                throw new IllegalArgumentException("Invalid proxy property: " + proxyProperty);
            }
            String host = splitStr[0].trim();
            int port = Integer.parseInt(splitStr[1].trim());
            return HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress(host, port))).build();
        }
    }

    @Test
    public void testAccessingRandomWordAPI() {
        String url = "https://random-word-api.herokuapp.com/word";
        try(HttpClient httpClient = createHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch data. HTTP Status Code: " + response.statusCode());
            }
            byte[] content = response.body();
            Assertions.assertNotNull(content);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<byte[]> generateRandomBytesFromRandomWordAPI(int elements) {
        String url = "https://random-word-api.herokuapp.com/word?number=" + elements;
        List<byte[]> result = new ArrayList<>();
        try(HttpClient httpClient = createHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch data. HTTP Status Code: " + response.statusCode());
            }
            byte[] content = response.body();
            String s = new String(content, StandardCharsets.UTF_8);
            if(!s.startsWith("[") || !s.endsWith("]")) {
                throw new RuntimeException("Corrupted data");
            }
            String[] parts = s.substring(1, s.length() - 1).split(",");
            for (String part : parts) {
                if(!part.startsWith("\"") || !part.endsWith("\"")) {
                    throw new RuntimeException("Corrupted data");
                }
                result.add(part.substring(1, part.length() - 1).getBytes(StandardCharsets.UTF_8));
            }
            return result;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    interface detector {
        int detect(List<byte[]> data);
    }

    private static void detectCollisions(int elements, detector detector) {
        int successfulCount = 0;
        int maxCollisions = -1;
        for(int i = 0; i < BATCH; i++) {
            List<byte[]> bytes = generateRandomBytesFromRandomWordAPI(elements);
            int collisions = detector.detect(bytes);
            if(collisions > maxCollisions) {
                maxCollisions = collisions;
            }
            if(collisions == 0) {
                successfulCount++;
            }
        }
        System.out.println("SuccessfulCount : " + successfulCount);
        System.out.println("SuccessfulRate : " + (successfulCount * 100.0 / BATCH) + "%");
        System.out.println("MaxCollisions : " + maxCollisions);
    }

    @Test
    public void testDetectLengthHashFor4Elements() {
        // 60%
        detectCollisions(4, HashUtil::lengthHashCollisions);
    }

    @Test
    public void testDetectLengthHashFor8Elements() {
        // 8%
        detectCollisions(8, HashUtil::lengthHashCollisions);
    }

    @Test
    public void testDetectLengthHashFor16Elements() {
        // 0%
        detectCollisions(16, HashUtil::lengthHashCollisions);
    }

    @Test
    public void testDetectFirstByteHashFor4Elements() {
        // 91%
        detectCollisions(4, HashUtil::firstByteHashCollisions);
    }

    @Test
    public void testDetectFirstByteHashFor8Elements() {
        // 61%
        detectCollisions(8, HashUtil::firstByteHashCollisions);
    }

    @Test
    public void testDetectFirstByteHashFor16Elements() {
        // 11%
        detectCollisions(16, HashUtil::firstByteHashCollisions);
    }
}
