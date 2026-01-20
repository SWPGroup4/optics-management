package com.glassystem.optics.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PUBLIC)
public class FileStorageService {
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    private final S3Client s3Client;


    public FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }
    String uploadFile(MultipartFile file, String folder) throws IOException {
        String fileName = folder+"/"+ UUID.randomUUID().toString() + "." + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putObjectRequest,  RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return String.format("https://%s.s3.amazonaws.com./%s", bucketName, fileName);
    }
}
