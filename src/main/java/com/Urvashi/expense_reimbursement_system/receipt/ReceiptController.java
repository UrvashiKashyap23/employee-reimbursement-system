package com.Urvashi.expense_reimbursement_system.receipt;

import com.Urvashi.expense_reimbursement_system.receipt.dto.ReceiptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReceiptController {

    private static final String OWN_CLAIMS_ROLES = "hasAnyRole('EMPLOYEE','MANAGER','FINANCE')";

    private final ReceiptService receiptService;

    // Upload: multipart/form-data with a part named "file"
    @PreAuthorize(OWN_CLAIMS_ROLES)
    @PostMapping(value = "/claims/{claimId}/receipts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptResponse> upload(@PathVariable Long claimId,
                                                  @RequestParam("file") MultipartFile file,
                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(receiptService.upload(claimId, file, authentication.getName()));
    }

    // Any logged-in user; the service checks that they may see this claim
    @GetMapping("/claims/{claimId}/receipts")
    public List<ReceiptResponse> list(@PathVariable Long claimId, Authentication authentication) {
        return receiptService.listForClaim(claimId, authentication.getName());
    }

    @GetMapping("/receipts/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
        ReceiptService.ReceiptFile file = receiptService.getFile(id, authentication.getName());

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")     // browser must not guess another type
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(file.resource());
    }

    @PreAuthorize(OWN_CLAIMS_ROLES)
    @DeleteMapping("/receipts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        receiptService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}