package com.laundry.modules.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laundry.common.security.JwtUtil;
import com.laundry.modules.order.application.dto.AddOrderItemImageRequest;
import com.laundry.modules.order.application.dto.AdjustPriceRequest;
import com.laundry.modules.order.application.dto.CancelOrderRequest;
import com.laundry.modules.order.application.dto.ChangeStatusRequest;
import com.laundry.modules.order.application.dto.CompleteOrderRequest;
import com.laundry.modules.order.application.dto.DeliverOrderRequest;
import com.laundry.modules.order.application.dto.OrderItemDto;
import com.laundry.modules.order.application.dto.QuickCreateOrderRequest;
import com.laundry.modules.order.application.dto.RescheduleOrderRequest;
import com.laundry.modules.order.application.dto.RevertStatusRequest;
import com.laundry.modules.order.application.dto.UpdateOrderItemsRequest;
import com.laundry.modules.order.domain.CodeSequenceRepository;
import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderRepository;
import com.laundry.modules.order.domain.OrderStatus;
import com.laundry.modules.order.domain.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderTests {

    @Autowired
    MockMvc mvc;

    final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    CodeSequenceRepository codeSequenceRepository;

    String staffToken;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        codeSequenceRepository.deleteAll();
        staffToken = "Bearer " + jwtUtil.generateToken("staff1", List.of("STAFF"));
    }

    @Test
    @DisplayName("Quy trình chuẩn M3: Mã LD- tuần tự -> Fast Check-in (RECEIVED) -> In phiếu -> Đang xử lý -> Hoàn thành (Gán kệ) -> Giao đồ (Nhận hộ)")
    void fullMember3LifecycleTest() throws Exception {
        // 1. UC04: Tiếp nhận đồ & tạo đơn (LD-YYYYMMDD-001)
        QuickCreateOrderRequest createReq = new QuickCreateOrderRequest(
                "Trần Thị B",
                "0912345678",
                ServiceType.WASH_DRY,
                BigDecimal.valueOf(5.5),
                BigDecimal.valueOf(15000),
                "Áo trắng giặt riêng, không dùng xả thơm",
                OrderPriority.NORMAL,
                null,
                1,
                null
        );

        MvcResult result1 = mvc.perform(post("/api/orders/quick-create")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderCode", startsWith("LD-")))
                .andExpect(jsonPath("$.orderCode", containsString("-001")))
                .andExpect(jsonPath("$.status", is("RECEIVED")))
                .andExpect(jsonPath("$.statusDescription", is("Mới nhận")))
                .andExpect(jsonPath("$.totalAmount").value(82500))
                .andReturn();

        Long orderId1 = objectMapper.readTree(result1.getResponse().getContentAsString()).get("id").asLong();

        // Kiểm tra tạo đơn thứ 2 để kiểm tra sinh mã tuần tự LD-YYYYMMDD-002
        QuickCreateOrderRequest createReq2 = new QuickCreateOrderRequest(
                "Khách Hàng Khác",
                "0988889999",
                ServiceType.WASH_DRY,
                BigDecimal.valueOf(2.0),
                BigDecimal.valueOf(15000),
                null,
                OrderPriority.NORMAL,
                null,
                1,
                null
        );
        mvc.perform(post("/api/orders/quick-create")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderCode", containsString("-002")));

        // 2. BM3: In phiếu nhận đồ (cập nhật receiptPrintedAt, có link QR)
        mvc.perform(get("/api/orders/" + orderId1 + "/receipt")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCode", containsString("-001")))
                .andExpect(jsonPath("$.totalWeight").value(5.5))
                .andExpect(jsonPath("$.trackingQrUrl", containsString("/track?phone=0912345678")));

        // 3. UC04: Phân loại chi tiết món đồ
        UpdateOrderItemsRequest itemsReq = new UpdateOrderItemsRequest(List.of(
                new OrderItemDto(null, null, "Áo sơ mi trắng", BigDecimal.valueOf(3), ItemUnit.PIECE, BigDecimal.valueOf(15000), BigDecimal.ZERO, BigDecimal.valueOf(45000), "Ố nhẹ cổ áo", List.of()),
                new OrderItemDto(null, null, "Quần tây đen", BigDecimal.valueOf(2), ItemUnit.PIECE, BigDecimal.valueOf(15000), BigDecimal.ZERO, BigDecimal.valueOf(30000), null, List.of())
        ));

        MvcResult updateItemsRes = mvc.perform(put("/api/orders/" + orderId1 + "/items")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemsReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();

        Long firstItemId = objectMapper.readTree(updateItemsRes.getResponse().getContentAsString()).get("items").get(0).get("id").asLong();

        // 4. order_images: Upload ảnh tình trạng món đồ có vết ố
        mvc.perform(post("/api/orders/" + orderId1 + "/items/" + firstItemId + "/images")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddOrderItemImageRequest("https://cdn.example.com/stain1.jpg", "Ảnh chụp vết ố cổ áo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl", is("https://cdn.example.com/stain1.jpg")));

        // 5. UC08: Cập nhật sang PROCESSING (Đang xử lý)
        mvc.perform(patch("/api/orders/" + orderId1 + "/status")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(OrderStatus.PROCESSING, "Bắt đầu cho vào máy giặt số 2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSING")));

        // 6. UC08 / QĐ6: Hoàn thành xử lý & Gán vị trí kệ
        mvc.perform(post("/api/orders/" + orderId1 + "/complete")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompleteOrderRequest("Kệ A-03"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.shelfLocation", is("Kệ A-03")))
                .andExpect(jsonPath("$.completedAt", notNullValue()));

        // 7. UC12 / QĐ9: Giao đồ cho người nhận hộ
        mvc.perform(post("/api/orders/" + orderId1 + "/deliver")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeliverOrderRequest("Nguyễn Văn Em", "Em trai"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DELIVERED")))
                .andExpect(jsonPath("$.receiverName", is("Nguyễn Văn Em")))
                .andExpect(jsonPath("$.receiverRelation", is("Em trai")))
                .andExpect(jsonPath("$.deliveredAt", notNullValue()));

        // 8. UC29 / BM6: Tra cứu công khai theo SĐT (Khách hàng xem)
        mvc.perform(get("/api/orders/track?phone=0912345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].shelfLocation", is("Kệ A-03")))
                .andExpect(jsonPath("$[0].status", is("DELIVERED")));
    }

    @Test
    @DisplayName("Nghiệp vụ ngoại lệ: Dời ngày hẹn (UC06), Sửa giá (UC21), Lùi trạng thái (UC22), Hủy đơn (UC05)")
    void exceptionWorkflowsTest() throws Exception {
        QuickCreateOrderRequest createReq = new QuickCreateOrderRequest(
                "Lê Văn C",
                "0933334444",
                ServiceType.EXPRESS,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(30000),
                "Cần gấp trong ngày",
                OrderPriority.URGENT,
                null,
                1,
                null
        );

        MvcResult result = mvc.perform(post("/api/orders/quick-create")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // 1. UC06 / BR-D08: Dời ngày hẹn sau khi in phiếu (bắt buộc lý do)
        Instant newDue = Instant.now().plus(48, ChronoUnit.HOURS);
        mvc.perform(patch("/api/orders/" + orderId + "/reschedule")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RescheduleOrderRequest(newDue, null, "Cúp điện 2 tiếng"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueAt", notNullValue()));

        // 2. UC21 / BR-D12: Sửa giá kèm lý do
        mvc.perform(patch("/api/orders/" + orderId + "/price")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustPriceRequest(BigDecimal.valueOf(110000), "Giảm giá đền bù dời hẹn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(110000));

        // 3. UC22 / BR-D06: Chuyển sang PROCESSING rồi lùi về RECEIVED
        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(OrderStatus.PROCESSING, "Bắt đầu giặt"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/orders/" + orderId + "/revert-status")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RevertStatusRequest(OrderStatus.RECEIVED, "Khách gọi nhờ giữ lại để gửi thêm đồ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RECEIVED")));

        // 4. UC05 / BR-D05: Hủy đơn khi Mới nhận (kèm lý do)
        mvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("Khách đổi ý mang đồ về"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }
}
