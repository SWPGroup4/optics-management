package com.glassystem.optics.service;

import com.glassystem.optics.enums.S3ImageName;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileStorageService {

    @Value("${aws.s3.bucket-name}")
    String bucketName;

    final S3Client s3Client;

    public FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file, S3ImageName folder) throws IOException {

        String safeFileName = file.getOriginalFilename()
                .replaceAll("\\s+", "_");

        String key = folder.name()
                + "/"
                + UUID.randomUUID()
                + "-"
                + safeFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return "https://" + bucketName + ".s3.amazonaws.com/" + key;
    }


    public void deleteFileByKey(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Deleted S3 object: {}", key);

        } catch (Exception e) {
            log.warn("Failed to delete old S3 file: {}", e.getMessage());
        }
    }

    private String extractKeyFromUrl(String url) {
        URI uri = URI.create(url);
        return uri.getPath().substring(1); // bỏ dấu /
    }
}
