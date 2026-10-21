package com.Urvashi.expense_reimbursement_system.receipt.dto;

import com.Urvashi.expense_reimbursement_system.receipt.Receipt;

import java.time.Instant;

public record ReceiptResponse(
        Long id,
        Long claimId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        Instant uploadedAt
) {

    public static ReceiptResponse from(Receipt receipt) {
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getClaim().getId(),
                receipt.getOriginalFilename(),
                receipt.getContentType(),
                receipt.getSizeBytes(),
                receipt.getUploadedAt()
        );
    }
}