package com.smartnav.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartnav.dto.LocationSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Map<String, List<LocationSearchResult>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, LocationSearchResult> reverseCache = new ConcurrentHashMap<>();

    // Curated high-precision offline Indian landmark gazetteer for zero-latency fallbacks
    private static final List<LocationSearchResult> GAZETTEER = List.of(
            // Pune
            new LocationSearchResult("Pune Railway Station, Agarkar Nagar, Pune, Maharashtra", "Pune Railway Station", 18.5289, 73.8744, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("Hinjewadi Phase 1, Rajiv Gandhi Infotech Park, Pune, Maharashtra", "Hinjewadi Phase 1", 18.5913, 73.7389, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("Shivajinagar, JM Road, Pune, Maharashtra", "Shivajinagar, Pune", 18.5314, 73.8446, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("FC Road (Fergusson College Road), Deccan Gymkhana, Pune, Maharashtra", "FC Road, Pune", 18.5246, 73.8415, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("Swargate Bus Stand, Satara Road, Pune, Maharashtra", "Swargate, Pune", 18.5018, 73.8586, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("Baner Road, Balewadi High Street, Pune, Maharashtra", "Baner Road, Pune", 18.5590, 73.7930, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("Senapati Bapat Road, ICC Tech Park, Pune, Maharashtra", "Senapati Bapat Road, Pune", 18.5308, 73.8289, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("Viman Nagar, Phoenix Marketcity, Pune, Maharashtra", "Viman Nagar, Pune", 18.5679, 73.9143, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("Kothrud, Paud Road, Pune, Maharashtra", "Kothrud, Pune", 18.5074, 73.8077, "Pune", "Maharashtra", "India"),
            new LocationSearchResult("Hadapsar, Magarpatta City, Pune, Maharashtra", "Hadapsar, Pune", 18.5089, 73.9260, "Pune", "Maharashtra", "India"),

            // Mumbai
            new LocationSearchResult("Chhatrapati Shivaji Maharaj International Airport (T2), Mumbai, Maharashtra", "Mumbai Airport (T2)", 19.0974, 72.8744, "Mumbai", "Maharashtra", "India"),
            new LocationSearchResult("Bandra Kurla Complex (BKC), Bandra East, Mumbai, Maharashtra", "Bandra Kurla Complex (BKC)", 19.0660, 72.8687, "Mumbai", "Maharashtra", "India"),
            new LocationSearchResult("Dadar TT Circle, Dadar East, Mumbai, Maharashtra", "Dadar TT Circle", 19.0178, 72.8478, "Mumbai", "Maharashtra", "India"),
            new LocationSearchResult("Bandra-Worli Sea Link Entrance, Mumbai, Maharashtra", "Bandra-Worli Sea Link", 19.0330, 72.8160, "Mumbai", "Maharashtra", "India"),
            new LocationSearchResult("Marine Drive, Nariman Point, Mumbai, Maharashtra", "Marine Drive, Mumbai", 18.9438, 72.8232, "Mumbai", "Maharashtra", "India"),
            new LocationSearchResult("Andheri Railway Station (East), Mumbai, Maharashtra", "Andheri Station, Mumbai", 19.1197, 72.8464, "Mumbai", "Maharashtra", "India"),

            // Bengaluru
            new LocationSearchResult("KSR Bengaluru City Railway Station (Majestic), Bengaluru, Karnataka", "Majestic / KSR Station", 12.9784, 77.5726, "Bengaluru", "Karnataka", "India"),
            new LocationSearchResult("Central Silk Board Junction, Hosur Road, Bengaluru, Karnataka", "Silk Board Junction", 12.9172, 77.6228, "Bengaluru", "Karnataka", "India"),
            new LocationSearchResult("Koramangala 80 Feet Road, Bengaluru, Karnataka", "Koramangala, Bengaluru", 12.9352, 77.6245, "Bengaluru", "Karnataka", "India"),
            new LocationSearchResult("Whitefield, ITPL Main Road, Bengaluru, Karnataka", "Whitefield, Bengaluru", 12.9698, 77.7499, "Bengaluru", "Karnataka", "India"),
            new LocationSearchResult("Indiranagar 100 Feet Road, Bengaluru, Karnataka", "Indiranagar, Bengaluru", 12.9719, 77.6412, "Bengaluru", "Karnataka", "India"),

            // Delhi
            new LocationSearchResult("India Gate, Kartavya Path, New Delhi, Delhi", "India Gate, Delhi", 28.6129, 77.2295, "Delhi", "Delhi", "India"),
            new LocationSearchResult("Connaught Place (CP), Rajiv Chowk, New Delhi, Delhi", "Connaught Place, Delhi", 28.6315, 77.2167, "Delhi", "Delhi", "India"),
            new LocationSearchResult("AIIMS Hospital & Flyover, Ring Road, New Delhi, Delhi", "AIIMS, Delhi", 28.5672, 77.2100, "Delhi", "Delhi", "India"),
            new LocationSearchResult("Dhaula Kuan Intersection, New Delhi, Delhi", "Dhaula Kuan, Delhi", 28.5921, 77.1610, "Delhi", "Delhi", "India"),

            // Hyderabad
            new LocationSearchResult("Cyber Towers, Hitec City, Madhapur, Hyderabad, Telangana", "Cyber Towers, Hitec City", 17.4504, 78.3808, "Hyderabad", "Telangana", "India"),
            new LocationSearchResult("Charminar, Old City, Hyderabad, Telangana", "Charminar, Hyderabad", 17.3616, 78.4747, "Hyderabad", "Telangana", "India"),
            new LocationSearchResult("Gachibowli Stadium & Flyover, Hyderabad, Telangana", "Gachibowli, Hyderabad", 17.4401, 78.3489, "Hyderabad", "Telangana", "India")
    );

    public LocationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    public List<LocationSearchResult> search(String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        String normalizedQuery = query.trim().toLowerCase();
        if (searchCache.containsKey(normalizedQuery)) {
            return searchCache.get(normalizedQuery);
        }

        List<LocationSearchResult> results = new ArrayList<>();

        // 1. Check local curated gazetteer first for exact/partial matches
        for (LocationSearchResult item : GAZETTEER) {
            if (item.getName().toLowerCase().contains(normalizedQuery) ||
                item.getDisplayName().toLowerCase().contains(normalizedQuery)) {
                results.add(item);
            }
        }

        // 2. Query Nominatim for live global OpenStreetMap coordinates
        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&q=" + encoded + "&limit=8";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SmartNav-Crowdsourced-System/1.0 (contact: student-project@smartnav.edu)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        String displayName = node.path("display_name").asText();
                        String name = node.path("name").asText(displayName.split(",")[0]);
                        double lat = node.path("lat").asDouble();
                        double lon = node.path("lon").asDouble();

                        JsonNode addressNode = node.path("address");
                        String city = addressNode.path("city").asText(
                                addressNode.path("town").asText(
                                        addressNode.path("suburb").asText("Unknown City")));
                        String state = addressNode.path("state").asText("Maharashtra");
                        String country = addressNode.path("country").asText("India");

                        // Avoid duplicate lat/lon
                        boolean exists = results.stream().anyMatch(r ->
                                Math.abs(r.getLat() - lat) < 0.001 && Math.abs(r.getLng() - lon) < 0.001);
                        if (!exists) {
                            results.add(new LocationSearchResult(displayName, name, lat, lon, city, state, country));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Nominatim geocoding lookup failed for '{}': {}", query, e.getMessage());
        }

        // Cache result (capped at 500 entries)
        if (searchCache.size() > 500) {
            searchCache.clear();
        }
        searchCache.put(normalizedQuery, results);
        return results;
    }

    public LocationSearchResult reverseGeocode(double lat, double lng) {
        String key = String.format(Locale.US, "%.4f,%.4f", lat, lng);
        if (reverseCache.containsKey(key)) {
            return reverseCache.get(key);
        }

        try {
            String url = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f", lat, lng);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SmartNav-Crowdsourced-System/1.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String displayName = root.path("display_name").asText();
                JsonNode addressNode = root.path("address");
                String city = addressNode.path("city").asText(
                        addressNode.path("town").asText(
                                addressNode.path("county").asText(
                                        addressNode.path("suburb").asText("Pune"))));
                String state = addressNode.path("state").asText("Maharashtra");
                String country = addressNode.path("country").asText("India");
                String name = root.path("name").asText(displayName.split(",")[0]);

                LocationSearchResult result = new LocationSearchResult(displayName, name, lat, lng, city, state, country);
                reverseCache.put(key, result);
                return result;
            }
        } catch (Exception e) {
            log.warn("Nominatim reverse geocode lookup failed: {}", e.getMessage());
        }

        // Fallback default
        LocationSearchResult fallback = new LocationSearchResult(
                String.format(Locale.US, "Location (%.4f, %.4f), India", lat, lng),
                "Selected Pin", lat, lng, "Pune", "Maharashtra", "India");
        reverseCache.put(key, fallback);
        return fallback;
    }
}
