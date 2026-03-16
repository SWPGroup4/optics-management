package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.response.PolicyResponse;
import com.glassystem.optics.entity.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PolicyMapper {

	@Mapping(target = "managerUserId", expression = "java(policy.getManagerUser() != null ? policy.getManagerUser().getId() : null)")
	@Mapping(target = "managerUsername", expression = "java(policy.getManagerUser() != null ? policy.getManagerUser().getUsername() : null)")
	PolicyResponse toPolicyResponse(Policy policy);
}
