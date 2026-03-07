package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.LensCreateRequest;
import com.glassystem.optics.dto.response.LensResponse;
import com.glassystem.optics.entity.Lens;
import com.glassystem.optics.mapper.LensMapper;
import com.glassystem.optics.repository.LensRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LensService {
    LensRepository lensRepository;
    LensMapper lensMapper;

    @Transactional
    public LensResponse createLens(LensCreateRequest request) {
        Lens lens = lensMapper.toLens(request);
        lens = lensRepository.save(lens);
        return lensMapper.toLensResponse(lens);
    }
}
