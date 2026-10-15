package com.laundry.modules.customer;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.customer.application.CustomerService;
import com.laundry.modules.customer.application.dto.CreateCustomerRequest;
import com.laundry.modules.customer.application.dto.CustomerResponse;
import com.laundry.modules.customer.application.dto.UpdateCustomerRequest;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.customer.domain.CustomerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("Tạo khách hàng mới thành công với đầy đủ thông tin hợp lệ")
    void createCustomer_Success() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Nguyễn Văn An",
                "0901234567",
                "123 Nguyễn Huệ, Q1, TP.HCM",
                "Khách thích giặt xả Comfort"
        );

        when(customerRepository.existsByPhone("0901234567")).thenReturn(false);
        when(customerRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(customerRepository.findByCustomerCode("KH000001")).thenReturn(Optional.empty());

        Customer savedCustomer = Customer.builder()
                .customerCode("KH000001")
                .fullName(request.fullName())
                .phone(request.phone())
                .address(request.address())
                .note(request.note())
                .isVip(false)
                .status(CustomerStatus.ACTIVE)
                .createdBy("admin")
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponse response = customerService.createCustomer(request, "admin");

        assertThat(response).isNotNull();
        assertThat(response.customerCode()).isEqualTo("KH000001");
        assertThat(response.fullName()).isEqualTo("Nguyễn Văn An");
        assertThat(response.phone()).isEqualTo("0901234567");
        assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(response.isVip()).isFalse();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Tạo khách hàng thất bại khi số điện thoại đã tồn tại (BR-B01)")
    void createCustomer_DuplicatePhone_ThrowsBusinessException() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Trần Thị Bình",
                "0912345678",
                "456 Lê Lợi",
                null
        );

        when(customerRepository.existsByPhone("0912345678")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request, "staff01"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã được đăng ký trong hệ thống (BR-B01)");
    }

    @Test
    @DisplayName("Tra cứu khách hàng theo SĐT thành công (Fast Check-in)")
    void getCustomerByPhone_Success() {
        Customer customer = Customer.builder()
                .customerCode("KH000010")
                .fullName("Lê Văn Cường")
                .phone("0988776655")
                .status(CustomerStatus.ACTIVE)
                .isVip(true)
                .build();

        when(customerRepository.findByPhone("0988776655")).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerByPhone("0988776655");

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Lê Văn Cường");
        assertThat(response.isVip()).isTrue();
    }

    @Test
    @DisplayName("Tra cứu khách hàng theo SĐT không tồn tại -> ném NotFoundException")
    void getCustomerByPhone_NotFound() {
        when(customerRepository.findByPhone("0999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerByPhone("0999999999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Không tìm thấy khách hàng");
    }

    @Test
    @DisplayName("Cập nhật thông tin khách hàng thành công")
    void updateCustomer_Success() {
        Customer existing = Customer.builder()
                .customerCode("KH000005")
                .fullName("Phạm Văn D")
                .phone("0933112233")
                .address("Địa chỉ cũ")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCustomerRequest updateReq = new UpdateCustomerRequest(
                "Phạm Văn Dũng",
                "0933112233",
                "Địa chỉ mới, Q3",
                "Ghi chú mới"
        );

        CustomerResponse response = customerService.updateCustomer(5L, updateReq, "admin");

        assertThat(response.fullName()).isEqualTo("Phạm Văn Dũng");
        assertThat(response.address()).isEqualTo("Địa chỉ mới, Q3");
        assertThat(response.note()).isEqualTo("Ghi chú mới");
    }

    @Test
    @DisplayName("Cập nhật số điện thoại bị trùng với khách khác -> ném BusinessException (BR-B01)")
    void updateCustomer_DuplicatePhone_ThrowsBusinessException() {
        Customer existing = Customer.builder()
                .customerCode("KH000005")
                .fullName("Phạm Văn D")
                .phone("0933112233")
                .build();

        when(customerRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByPhoneAndIdNot("0977889900", 5L)).thenReturn(true);

        UpdateCustomerRequest updateReq = new UpdateCustomerRequest(
                "Phạm Văn D",
                "0977889900",
                null,
                null
        );

        assertThatThrownBy(() -> customerService.updateCustomer(5L, updateReq, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã thuộc về một khách hàng khác (BR-B01)");
    }

    @Test
    @DisplayName("Chuyển trạng thái khách hàng sang INACTIVE (vô hiệu hóa thay cho xóa BR-B03)")
    void changeStatus_Deactivate_Success() {
        Customer existing = Customer.builder()
                .customerCode("KH000005")
                .fullName("Khách hàng A")
                .phone("0900112233")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.changeStatus(5L, CustomerStatus.INACTIVE);

        assertThat(response.status()).isEqualTo(CustomerStatus.INACTIVE);
    }

    @Test
    @DisplayName("Tìm kiếm phân trang danh sách khách hàng")
    void listCustomers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Customer c1 = Customer.builder().customerCode("KH000001").fullName("An").phone("0901").build();
        Customer c2 = Customer.builder().customerCode("KH000002").fullName("Bình").phone("0902").build();

        when(customerRepository.searchCustomers("090", pageable))
                .thenReturn(new PageImpl<>(List.of(c1, c2), pageable, 2));

        Page<CustomerResponse> page = customerService.listCustomers("090", pageable);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }
}
