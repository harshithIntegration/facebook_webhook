package com.webhooks.webhooks;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Value("${app.token}")
    private String token;

    private static final String VERIFY_TOKEN = "your_verify_token";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Verify the webhook subscription
    @GetMapping
    public String verifyWebhook(@RequestParam("hub.mode") String mode, 
                                @RequestParam("hub.verify_token") String token, 
                                @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return challenge;
        } else {
            return "Verification failed";
        }
    }

    // Handle incoming webhook payload
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            if (jsonNode.has("entry")) {
                JsonNode entries = jsonNode.get("entry");
                for (JsonNode entry : entries) {
                    // Process each entry
                    System.out.println("Entry: " + entry);
                    // Further processing based on the event type
                    processEntry(entry);
                }
                return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>("No valid message", HttpStatus.BAD_REQUEST);
    }

    // Method to process each entry
    private void processEntry(JsonNode entry) {
        // Get the "changes" list from the first entry
        List<JsonNode> changes = entry.findValues("changes");
        for (JsonNode change : changes) {
            JsonNode value = change.get("value");

            if (value != null && value.has("messages")) {
                // 'messages' is a list, get the first message
                List<JsonNode> messages = value.findValues("messages");
                if (messages != null && !messages.isEmpty()) {
                    // Get the first message in the messages list
                    JsonNode message = messages.get(0);

                    // Get the required fields: phone_number_id, from, and message body
                    String phoneNumberId = value.get("metadata").get("phone_number_id").asText();
                    String from = message.get("from").asText();
                    String msgBody = message.get("text").get("body").asText();

                    // Log the values
                    System.out.println("Phone number: " + phoneNumberId);
                    System.out.println("From: " + from);
                    System.out.println("Message body: " + msgBody);

                    // Send a message response back via WhatsApp API
                    sendMessageToWhatsapp(phoneNumberId, from, msgBody);
                }
            }
        }
    }

    // Function to send message back via WhatsApp API
    private void sendMessageToWhatsapp(String phoneNumberId, String from, String messageBody) {
        String url = "https://graph.facebook.com/v13.0/" + phoneNumberId + "/messages?access_token=" + token;

        RestTemplate restTemplate = new RestTemplate();

        // Create request body to send the response message
        Map<String, Object> requestBody = Map.of(
            "messaging_product", "whatsapp",
            "to", from,
            "text", Map.of("body", "At Quantum Paradigm, we specialize in cutting-edge backend development solutions, leveraging advanced technologies like Spring Boot and Java to deliver scalable, efficient, and high-performance systems tailored to meet the evolving needs of businesses across industries: " + messageBody)
        );

        // Set headers for the HTTP POST request
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Create HTTP entity (body + headers)
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Execute HTTP POST request to WhatsApp API
        restTemplate.postForObject(url, entity, String.class);
    }
}
