package com.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

class TestModel extends BaseModel {
    private Map<String, Object> data = new HashMap<>();
    
    public TestModel(String objName) {
        this.obj_name = objName;
    }
    
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    @Override
    public Object get(String fieldName) {
        return data.get(fieldName);
    }
}

public class Main {
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Load schemas from JSON files in resources
            InputStream schemaStream = Main.class.getClassLoader().getResourceAsStream("interface.json");
            InputStream servicesStream = Main.class.getClassLoader().getResourceAsStream("services.json");

            if (schemaStream == null || servicesStream == null) {
                throw new RuntimeException("JSON files not found in resources.");
            }

            List<Map<String, Object>> schema = objectMapper.readValue(schemaStream, new TypeReference<>() {});
            List<Map<String, Object>> servicesSchema = objectMapper.readValue(servicesStream, new TypeReference<>() {});

            // Create parser
            Parser parser = new Parser(schema, servicesSchema);

            // Create a test model
            TestModel person = new TestModel("Person");
            person.put("name", "John Doe");
            person.put("age", 30);
            person.put("height", 1.75f);
            person.put("isStudent", true);

            // Marshall the model
            UUID requestId = UUID.randomUUID();
            int serviceId = 1;
            boolean isRequest = true;

            byte[] bytes = parser.marshall(requestId, serviceId, isRequest, person);

            // Unmarshall the bytes
            Map<String, Object> result = parser.unmarshall(bytes);

            // Print the result
            System.out.println("Unmarshalled data:");
            result.forEach((key, value) -> System.out.println(key + ": " + value));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
