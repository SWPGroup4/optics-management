package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.LensCreateRequest;
import com.glassystem.optics.dto.response.LensResponse;
import com.glassystem.optics.dto.response.ProductResponse;
import com.glassystem.optics.entity.Lens;
import com.glassystem.optics.entity.Product;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.LensMapper;
import com.glassystem.optics.repository.LensRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<LensResponse> getLenses() {
        return lensRepository.findAll().stream().map(lensMapper::toLensResponse).toList();
    }

    public LensResponse getById(String id) {
        Lens len = lensRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LENS_NOT_FOUND));
        return lensMapper.toLensResponse(len);
    }
}
