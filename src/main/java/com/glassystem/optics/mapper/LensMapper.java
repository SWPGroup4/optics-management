package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.request.LensCreateRequest;
import com.glassystem.optics.dto.response.LensResponse;
import com.glassystem.optics.entity.Lens;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LensMapper {
    Lens toLens(LensCreateRequest request);

    LensResponse toLensResponse(Lens lens);
}
