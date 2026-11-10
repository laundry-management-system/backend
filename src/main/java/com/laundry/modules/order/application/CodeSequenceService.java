package com.laundry.modules.order.application;

import com.laundry.modules.order.domain.CodeSequence;
import com.laundry.modules.order.domain.CodeSequenceRepository;
import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.domain.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CodeSequenceService {

    private static final String DEFAULT_ORDER_PREFIX = "LD";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CodeSequenceRepository codeSequenceRepository;
    private final SettingService settingService;

    /** Mã đơn {ORDER_CODE_PREFIX}-yyyyMMdd-NNN; tiền tố lấy từ cài đặt, mặc định "LD". */
    @Transactional(propagation = Propagation.MANDATORY)
    public String getNextOrderCode() {
        return next("ORDER", settingService.getString(SettingKey.ORDER_CODE_PREFIX, DEFAULT_ORDER_PREFIX).trim());
    }

    /** Mã lượt nhận NH-yyyyMMdd-NNN: chung cho các đơn tạo trong một lần khách mang đồ đến. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String getNextIntakeCode() {
        return next("INTAKE", "NH");
    }

    /** Mã phiếu sự cố SC-yyyyMMdd-NNN. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String getNextIncidentCode() {
        return next("INCIDENT", "SC");
    }

    private String next(String seqType, String prefix) {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        CodeSequence sequence = codeSequenceRepository.findBySeqTypeAndSeqDateForUpdate(seqType, today)
                .orElseGet(() -> CodeSequence.builder()
                        .seqType(seqType)
                        .seqDate(today)
                        .lastNumber(0)
                        .build());

        int nextNum = sequence.increment();
        codeSequenceRepository.save(sequence);

        String dateStr = today.format(DATE_FORMATTER);
        return String.format("%s-%s-%03d", prefix, dateStr, nextNum);
    }
}
