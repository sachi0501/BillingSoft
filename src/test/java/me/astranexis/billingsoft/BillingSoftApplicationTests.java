package me.astranexis.billingsoft;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class BillingSoftApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void createsInvoiceAndRecordsPayment() throws Exception {
        Long productId = createProduct();
        Long customerId = createCustomer();

        String invoiceBody = """
                {
                  "customerId": %d,
                  "items": [
                    { "productId": %d, "quantity": 2 }
                  ],
                  "discountAmount": 10,
                  "notes": "Counter sale"
                }
                """.formatted(customerId, productId);

        String invoiceJson = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(200.00))
                .andExpect(jsonPath("$.taxTotal").value(36.00))
                .andExpect(jsonPath("$.grandTotal").value(226.00))
                .andExpect(jsonPath("$.status").value("UNPAID"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long invoiceId = objectMapper.readTree(invoiceJson).get("id").asLong();

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(3));

        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 100,
                                  "paymentMethod": "CASH",
                                  "referenceNumber": "CASH-001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00));

        mockMvc.perform(get("/api/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAmount").value(100.00))
                .andExpect(jsonPath("$.balanceDue").value(126.00))
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));

        mockMvc.perform(patch("/api/invoices/{id}/cancel", invoiceId))
                .andExpect(status().isBadRequest());
    }

    private Long createProduct() throws Exception {
        String json = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-100",
                                  "name": "Thermal Paper Roll",
                                  "description": "57mm billing roll",
                                  "unitPrice": 100,
                                  "taxRate": 18,
                                  "stockQuantity": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readId(json);
    }

    private Long createCustomer() throws Exception {
        String json = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Astra Retail",
                                  "email": "billing@example.com",
                                  "phone": "9999999999",
                                  "address": "Main Market",
                                  "taxNumber": "GSTIN123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readId(json);
    }

    private Long readId(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }

}
