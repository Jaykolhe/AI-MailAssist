package com.email.writer.service;

import com.email.writer.dto.EmailRequest;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClient) {
        this.webClient = WebClient.builder().build();
    }


    public String generateEmailReply(EmailRequest emailRequest) {

        //Build the Prompt

        String prompt = buildPrompt(emailRequest);

        //Craft a request
        Map<String,Object> requestBody = Map.of(
                "contents", new Object[]{
                   Map.of("parts",new Object[]{
                           Map.of("text",prompt)
                   })
        }
        );

        //Do request and Get response
        String response = webClient.post()
                .uri(geminiApiUrl +geminiApiKey)
                .header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Extract Response and return response

        return extractResponseContent(response);


    }

    private String extractResponseContent(String response) {

        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();


        } catch (Exception e) {
                return "Error Processing Request: "+e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a Professional Email reply for the following email Contect. please don't generate a subject line ");
        if(emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" Tone.");
        }

        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());

        return prompt.toString();
    }


}
