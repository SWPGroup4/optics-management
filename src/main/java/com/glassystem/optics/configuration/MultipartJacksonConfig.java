package com.glassystem.optics.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

import java.lang.reflect.Type;

@Configuration
public class MultipartJacksonConfig extends AbstractJackson2HttpMessageConverter {
    /**
     * Cấu hình này cho phép Spring xử lý các part có Content-Type là application/octet-stream
     * (mặc định của Swagger/Postman cho text part trong multipart) như là JSON.
     */
    public MultipartJacksonConfig(ObjectMapper objectMapper) {
        super(objectMapper, MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) { return false; }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) { return false; }

    @Override
    protected boolean canWrite(MediaType mediaType) { return false; }
}
