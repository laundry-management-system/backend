package com.laundry.modules.order.application;

import com.laundry.modules.order.domain.CodeSequence;
import com.laundry.modules.order.domain.CodeSequenceRepository;
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

    private final CodeSequenceRepository codeSequenceRepository;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional(propagation = Propagation.MANDATORY)
    public String getNextOrderCode() {
        return next("ORDER", "LD");
    }

    /** Mã lượt nhận NH-yyyyMMdd-NNN: chung cho các đơn tạo trong một lần khách mang đồ đến. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String getNextIntakeCode() {
        return next("INTAKE", "NH");
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

