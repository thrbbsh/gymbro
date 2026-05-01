package com.example.gymbro;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@SpringBootApplication
public class GymBroServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GymBroServerApplication.class, args);
    }
}

@RestController
class ExerciseController {

    @Value("${rapidapi.key}")
    private String rapidApiKey;

    private static final String DATA_FILE = "exercises.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/api/exercises")
    public ResponseEntity<?> getExercises() {
        List<Map<String, Object>> allExercises = loadLocalExercises();
        
        // If we have very few exercises, force a comprehensive sync
        if (allExercises.size() < 100) {
            allExercises = fullSyncWithRapidApi();
        }

        return ResponseEntity.ok(allExercises);
    }

    private List<Map<String, Object>> fullSyncWithRapidApi() {
        System.out.println("Starting comprehensive sync with ExerciseDB...");
        Map<String, Map<String, Object>> uniqueExercises = new HashMap<>();
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", rapidApiKey);
        headers.set("X-RapidAPI-Host", "exercisedb.p.rapidapi.com");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        int offset = 0;
        int limit = 50; // Use a reasonable limit per request
        boolean hasMore = true;

        while (hasMore && offset < 1000) { // Safety limit of 1000 exercises
            try {
                String url = String.format("https://exercisedb.p.rapidapi.com/exercises?limit=%d&offset=%d", limit, offset);
                System.out.println("Fetching: " + url);
                
                ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
                List<Map<String, Object>> batch = response.getBody();

                if (batch == null || batch.isEmpty()) {
                    hasMore = false;
                } else {
                    for (Map<String, Object> ex : batch) {
                        String id = ex.get("id").toString();
                        // Point gifUrl to our server proxy
                        ex.put("gifUrl", "http://10.0.2.2:3000/api/image?exerciseId=" + id);
                        uniqueExercises.put(id, ex);
                    }
                    offset += batch.size();
                    System.out.println("Progress: " + uniqueExercises.size() + " exercises collected.");
                    
                    // If we got fewer than requested, we might have reached the end or a plan limit
                    if (batch.size() < limit) {
                        // We don't stop immediately because some plans always return 10
                        // But if it's 0, we definitely stop.
                        if (batch.size() == 0) hasMore = false;
                    }
                    
                    Thread.sleep(200); // Respect API rate limits
                }
            } catch (Exception e) {
                System.err.println("Batch fetch failed at offset " + offset + ": " + e.getMessage());
                hasMore = false;
            }
        }

        List<Map<String, Object>> finalList = new ArrayList<>(uniqueExercises.values());
        saveLocalExercises(finalList);
        return finalList;
    }

    @GetMapping("/api/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String exerciseId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RapidAPI-Key", rapidApiKey);
            headers.set("X-RapidAPI-Host", "exercisedb.p.rapidapi.com");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = String.format("https://exercisedb.p.rapidapi.com/image?exerciseId=%s&resolution=360", exerciseId);
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            
            return ResponseEntity.ok()
                    .contentType(response.getHeaders().getContentType())
                    .body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private List<Map<String, Object>> loadLocalExercises() {
        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) return new ArrayList<>();
            return objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void saveLocalExercises(List<Map<String, Object>> exercises) {
        try {
            objectMapper.writeValue(new File(DATA_FILE), exercises);
        } catch (IOException e) {
            System.err.println("Failed to save exercises.json");
        }
    }
}
