package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.swing.text.Keymap;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailGeneratorService {

    //WebClient is a non-blocking, reactive HTTP client introduced in Spring WebFlux dependency
    //It is used to make HTTP requests to external APIs and web services.
    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest){
        //Build the prompt
        String prompt = buildPrompt(emailRequest);

        //Builde a request
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts",new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );

        //Do request and get response
        String response = webClient.post()
                .uri(geminiApiUrl+geminiApiKey)
                .header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        //Extract response and return
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try{
            //This code is used to parse a JSON string into a JSON tree structure using Jackson, a popular JSON processing library in Java.

            //ObjectMapper is a Jackson class used to convert JSON to Java objects and vice versa.
            //It allows reading, writing, and processing JSON data efficiently.
            ObjectMapper mapper=new ObjectMapper();

            //readTree(response) parses the JSON string (response) and converts it into a JsonNode.
            //JsonNode represents a JSON tree structure that allows easy navigation and extraction of values.
            JsonNode rootNode=mapper.readTree(response);

            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }catch (Exception e){
            return "Error processing request: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a proffesional email reply for the following email content. Please don't generate a subject line ");
        if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
        }
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
