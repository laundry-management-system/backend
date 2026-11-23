package com.laundry.modules.order.application;

import com.laundry.modules.order.domain.CodeSequence;
import com.laundry.modules.order.domain.CodeSequenceRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CodeSequenceService {

    private final CodeSequenceRepository codeSequenceRepository;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final LocalDate INCIDENT_SEQ_DATE = LocalDate.of(1970, 1, 1);

    @Transactional(propagation = Propagation.MANDATORY)
    public String getNextOrderCode() {
        return next("ORDER", "LD");
    }

    /** Mã lượt nhận NH-yyyyMMdd-NNN: chung cho các đơn tạo trong một lần khách mang đồ đến. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String getNextIntakeCode() {
        return next("INTAKE", "NH");
    }

    /** Mã phiếu sự cố SC-NNNN: một bộ đếm toàn hệ thống (ngày cố định), không đặt lại theo ngày. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String getNextIncidentCode() {
        return String.format("SC-%04d", nextNumber("INCIDENT", INCIDENT_SEQ_DATE));
    }

    private String next(String seqType, String prefix) {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        int nextNum = nextNumber(seqType, today);
        return String.format("%s-%s-%03d", prefix, today.format(DATE_FORMATTER), nextNum);
    }

    private int nextNumber(String seqType, LocalDate date) {
        codeSequenceRepository.insertIfAbsent(seqType, date);
        CodeSequence sequence = codeSequenceRepository.findBySeqTypeAndSeqDateForUpdate(seqType, date)
                .orElseThrow();
        int nextNum = sequence.increment();
        codeSequenceRepository.save(sequence);
        return nextNum;
    }
}
