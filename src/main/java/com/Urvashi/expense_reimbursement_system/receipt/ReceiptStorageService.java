package com.Urvashi.expense_reimbursement_system.receipt;

import com.Urvashi.expense_reimbursement_system.common.exception.BusinessRuleException;
import com.Urvashi.expense_reimbursement_system.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
public class ReceiptStorageService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    // The allowed file types. We decide the type from the file's real first bytes.
    private enum FileType {
        PDF("application/pdf", ".pdf"),
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png");

        final String mimeType;
        final String extension;

        FileType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }
    }

    // What the caller gets back after a file is saved
    public record StoredFile(String storedFilename, String contentType, long sizeBytes, String sha256Hash) {
    }

    private final Path root;

    public ReceiptStorageService(@Value("${app.receipts.storage-dir:uploads/receipts}") String storageDir) {
        this.root = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create receipt storage folder: " + root, e);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Receipt file is missing or empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessRuleException("Receipt file must be 5 MB or smaller", HttpStatus.valueOf(413));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded file", e);
        }

        FileType type = detectType(bytes);
        String storedFilename = UUID.randomUUID() + type.extension;
        Path target = root.resolve(storedFilename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Invalid storage path");
        }

        try {
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save receipt file", e);
        }
        return new StoredFile(storedFilename, type.mimeType, bytes.length, sha256(bytes));
    }

    public Resource load(String storedFilename) {
        Path path = root.resolve(storedFilename).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Receipt file not found on the server");
        }
        return new FileSystemResource(path);
    }

    public void deleteQuietly(String storedFilename) {
        try {
            Path path = root.resolve(storedFilename).normalize();
            if (path.startsWith(root)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("Could not delete receipt file {}", storedFilename, e);
        }
    }

    // ---------- private ----------

    private FileType detectType(byte[] b) {
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return FileType.PDF;
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return FileType.JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return FileType.PNG;
        }
        throw new BusinessRuleException("Only PDF, JPEG and PNG receipts are allowed");
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}