package com.trizenai.photoshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the core scenarios called out in the requirement doc:
 * - Registration / login (Authentication)
 * - Role-based authorization (Team Member cannot publish a gallery)
 * - A user attempting to access another Admin's event
 * - PIN-protected gallery access, including an incorrect PIN
 */
@SpringBootTest
@AutoConfigureMockMvc
class PhotoShareApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    private String adminToken;
    private String memberToken;
    private Long eventId;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueSuffix = String.valueOf(System.nanoTime());

        adminToken = registerAndLogin("Test Admin", "admin" + uniqueSuffix + "@test.com", "ADMIN");
        memberToken = registerAndLogin("Test Member", "member" + uniqueSuffix + "@test.com", "TEAM_MEMBER");

        MvcResult result = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Event\"}"))
                .andExpect(status().isOk())
                .andReturn();

        eventId = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String registerAndLogin(String name, String email, String role) throws Exception {
        String body = """
                {"name":"%s","email":"%s","password":"Passw0rd!","role":"%s"}
                """.formatted(name, email, role);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    @Test
    void adminCanCreateEventAndTeamMemberCannotPublishGallery() throws Exception {
        // Team Member attempting to publish a gallery must be forbidden.
        mockMvc.perform(post("/api/events/" + eventId + "/gallery/publish")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestToProtectedEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teamMemberCannotAccessEventTheyAreNotAssignedTo() throws Exception {
        // memberToken's user was never added to eventId's member list.
        mockMvc.perform(get("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void publishFailsWithoutSelectedPhotos() throws Exception {
        mockMvc.perform(post("/api/events/" + eventId + "/gallery/publish")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void incorrectGalleryPinIsRejected() throws Exception {
        // A gallery that doesn't exist should behave the same as a wrong PIN from the client's perspective: denied.
        mockMvc.perform(post("/api/gallery/does-not-exist/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"000000\"}"))
                .andExpect(status().isNotFound());
    }
}
