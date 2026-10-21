package com.Urvashi.expense_reimbursement_system.receipt;

import com.Urvashi.expense_reimbursement_system.audit.AuditAction;
import com.Urvashi.expense_reimbursement_system.audit.AuditService;
import com.Urvashi.expense_reimbursement_system.claim.ClaimService;
import com.Urvashi.expense_reimbursement_system.claim.ClaimWorkflow;
import com.Urvashi.expense_reimbursement_system.claim.ExpenseClaim;
import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.common.exception.ResourceNotFoundException;
import com.Urvashi.expense_reimbursement_system.receipt.dto.ReceiptResponse;
import com.Urvashi.expense_reimbursement_system.user.User;
import com.Urvashi.expense_reimbursement_system.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final int MAX_RECEIPTS_PER_CLAIM = 5;

    private final ReceiptRepository receiptRepository;
    private final ReceiptStorageService storageService;
    private final ClaimService claimService;
    private final UserService userService;
    private final AuditService auditService;

    // What the controller needs to send a file back
    public record ReceiptFile(String originalFilename, String contentType, Resource resource) {
    }

    @Transactional
    public ReceiptResponse upload(Long claimId, MultipartFile file, String email) {
        User user = userService.getEntityByEmail(email);
        ExpenseClaim claim = claimService.getAccessibleEntity(claimId, email);
        requireOwnerAndEditable(claim, user);

        if (receiptRepository.countByClaimId(claimId) >= MAX_RECEIPTS_PER_CLAIM) {
            throw new BusinessRuleException(
                    "A claim can have at most " + MAX_RECEIPTS_PER_CLAIM + " receipts", HttpStatus.CONFLICT);
        }

        ReceiptStorageService.StoredFile stored = storageService.store(file);

        // If the database transaction does not commit, remove the file we just wrote (no orphan files)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storageService.deleteQuietly(stored.storedFilename());
                }
            }
        });

        Receipt receipt = new Receipt();
        receipt.setClaim(claim);
        receipt.setOriginalFilename(cleanFilename(file.getOriginalFilename()));
        receipt.setStoredFilename(stored.storedFilename());
        receipt.setContentType(stored.contentType());
        receipt.setSizeBytes(stored.sizeBytes());
        receipt.setSha256Hash(stored.sha256Hash());
        Receipt saved = receiptRepository.save(receipt);

        // We never log file contents, only the name and size
        auditService.record(user, AuditAction.RECEIPT_UPLOADED, "CLAIM", claimId,
                "Receipt '" + saved.getOriginalFilename() + "' uploaded (" + saved.getSizeBytes() + " bytes)");
        return ReceiptResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> listForClaim(Long claimId, String email) {
        ExpenseClaim claim = claimService.getAccessibleEntity(claimId, email);   // permission check
        return receiptRepository.findByClaimIdOrderByUploadedAtAsc(claim.getId()).stream()
                .map(ReceiptResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReceiptFile getFile(Long receiptId, String email) {
        Receipt receipt = findReceipt(receiptId);
        claimService.getAccessibleEntity(receipt.getClaim().getId(), email);     // permission check
        Resource resource = storageService.load(receipt.getStoredFilename());
        return new ReceiptFile(receipt.getOriginalFilename(), receipt.getContentType(), resource);
    }

    @Transactional
    public void delete(Long receiptId, String email) {
        User user = userService.getEntityByEmail(email);
        Receipt receipt = findReceipt(receiptId);
        ExpenseClaim claim = claimService.getAccessibleEntity(receipt.getClaim().getId(), email);
        requireOwnerAndEditable(claim, user);

        String storedFilename = receipt.getStoredFilename();
        String originalFilename = receipt.getOriginalFilename();
        receiptRepository.delete(receipt);

        auditService.record(user, AuditAction.CLAIM_EDITED, "CLAIM", claim.getId(),
                "Receipt removed: '" + originalFilename + "'");

        // Delete the file only after the database change is safely committed
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storageService.deleteQuietly(storedFilename);
            }
        });
    }

    // ---------- private ----------

    private Receipt findReceipt(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", id));
    }

    private void requireOwnerAndEditable(ExpenseClaim claim, User user) {
        if (!claim.getEmployee().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the claim owner can change its receipts");
        }
        if (!ClaimWorkflow.isEditable(claim.getStatus())) {
            throw new BusinessRuleException(
                    "Receipts can only be changed while the claim is DRAFT or REJECTED. This claim is "
                            + claim.getStatus(), HttpStatus.CONFLICT);
        }
    }

    // Keep only a safe display name: no folder parts, no control characters, limited length
    private String cleanFilename(String original) {
        if (original == null || original.isBlank()) {
            return "receipt";
        }
        String name = original.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            return "receipt";
        }
        return name.length() > 150 ? name.substring(0, 150) : name;
    }
}