package com.webhooks.webhooks;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final String VERIFY_TOKEN = "your_verify_token";
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @PostMapping
    public void handleWebhook(@RequestBody String payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            if (jsonNode.has("entry")) {
                JsonNode entries = jsonNode.get("entry");
                for (JsonNode entry : entries) {
                    // Process each entry
                    System.out.println("Entry: " + entry);
                    // Further processing based on the event type
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}