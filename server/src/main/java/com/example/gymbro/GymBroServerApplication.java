package com.example.gymbro;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
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
        List<Object> allExercises = loadLocalExercises();
        
        System.out.println("Starting sync. Current local cache size: " + allExercises.size());

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RapidAPI-Key", rapidApiKey);
            headers.set("X-RapidAPI-Host", "exercisedb.p.rapidapi.com");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            boolean hasMore = true;
            while (hasMore) {
                int offset = allExercises.size();
                System.out.println("Fetching from RapidAPI: limit=10, offset=" + offset);
                
                String url = String.format("https://exercisedb.p.rapidapi.com/exercises?limit=10&offset=%d", offset);
                
                try {
                    ResponseEntity<List> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            List.class
                    );

                    List<Object> newBatch = response.getBody();
                    if (newBatch == null || newBatch.isEmpty()) {
                        hasMore = false;
                        System.out.println("No more data to fetch.");
                    } else {
                        allExercises.addAll(newBatch);
                        saveLocalExercises(allExercises); // Сохраняем сразу после получения порции
                        
                        if (newBatch.size() < 10) {
                            hasMore = false;
                        }
                        
                        // Небольшая задержка, чтобы не спамить API слишком быстро
                        Thread.sleep(200); 
                    }
                } catch (HttpClientErrorException.TooManyRequests e) {
                    System.err.println("RapidAPI LIMIT REACHED! Stopping and keeping current data.");
                    hasMore = false;
                }
            }

            return ResponseEntity.ok(allExercises);

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return ResponseEntity.ok(allExercises); // В любой непонятной ситуации отдаем то, что уже есть
        }
    }

    private List<Object> loadLocalExercises() {
        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) return new ArrayList<>();
            return objectMapper.readValue(file, new TypeReference<List<Object>>() {});
        } catch (IOException e) {
            System.err.println("Error reading cache: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveLocalExercises(List<Object> exercises) {
        try {
            objectMapper.writeValue(new File(DATA_FILE), exercises);
            System.out.println("Saved! Total exercises in cache: " + exercises.size());
        } catch (IOException e) {
            System.err.println("Failed to save to file: " + e.getMessage());
        }
    }

    @GetMapping("/api/community/posts")
    public String getCommunityPosts() {
        return "[{\"id\": 1, \"user\": \"GymRat\", \"content\": \"Just hit a new PR!\", \"likes\": 12}]";
    }
}
