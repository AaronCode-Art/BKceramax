package com.ceramax.api.controller.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UbigeoPublicoControllerTest {

    @Test
    void returnsNestedDepartmentProvinceAndDistrictCodesAsJson() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UbigeoPublicoController()).build();
        String body = mvc.perform(get("/api/v1/public/ubigeos"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode departments = new ObjectMapper().readTree(body);
        assertTrue(departments.isArray());

        JsonNode lima = findById(departments, "15");
        assertNotNull(lima);
        assertEquals("Lima", lima.path("name").asText());

        JsonNode limaProvince = findById(lima.path("childrens"), "1501");
        assertNotNull(limaProvince);
        assertEquals("Lima", limaProvince.path("name").asText());

        JsonNode limaDistrict = findById(limaProvince.path("childrens"), "150101");
        assertNotNull(limaDistrict);
        assertEquals("Lima", limaDistrict.path("name").asText());
    }

    private JsonNode findById(JsonNode options, String id) {
        for (JsonNode option : options) {
            if (id.equals(option.path("id").asText())) return option;
        }
        return null;
    }
}
