package com.laundry.modules.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "code_sequences")
@IdClass(CodeSequenceId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CodeSequence {

    @Id
    @Column(nullable = false, length = 10)
    private String seqType;

    @Id
    @Column(nullable = false)
    private LocalDate seqDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer lastNumber = 0;

    public int increment() {
        if (this.lastNumber == null) {
            this.lastNumber = 0;
        }
        this.lastNumber += 1;
        return this.lastNumber;
    }
}

