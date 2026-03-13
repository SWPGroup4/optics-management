package com.glassystem.optics.service;

import com.glassystem.optics.dto.request.PolicyCreateRequest;
import com.glassystem.optics.dto.request.PolicyUpdateRequest;
import com.glassystem.optics.dto.response.PolicyPageResponse;
import com.glassystem.optics.dto.response.PolicyResponse;
import com.glassystem.optics.entity.Policy;
import com.glassystem.optics.entity.User;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.PolicyMapper;
import com.glassystem.optics.repository.PolicyRepository;
import com.glassystem.optics.repository.UserRepository;
import com.glassystem.optics.specification.PolicySpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PolicyService {
	PolicyRepository policyRepository;
	UserRepository userRepository;
	PolicyMapper policyMapper;

	// =====================================================================
	// 1. TẠO POLICY (POST /api/policies)
	// =====================================================================

	@Transactional
	public PolicyResponse createPolicy(PolicyCreateRequest request) {
		// Validate code unique
		if (policyRepository.existsByCode(request.getCode())) {
			throw new AppException(ErrorCode.POLICY_CODE_ALREADY_EXISTS);
		}

		// Validate date range
		validateDateRange(request.getEffectiveFrom(), request.getEffectiveTo());

		// Lấy manager user từ security context
		User managerUser = getCurrentUser();

		Policy policy = Policy.builder()
				.managerUser(managerUser)
				.code(request.getCode())
				.title(request.getTitle())
				.description(request.getDescription())
				.effectiveFrom(request.getEffectiveFrom())
				.effectiveTo(request.getEffectiveTo())
				.build();

		policy = policyRepository.save(policy);

		log.info("Tạo policy thành công: id={}, code={}", policy.getId(), policy.getCode());
		return policyMapper.toPolicyResponse(policy);
	}

	// =====================================================================
	// 2. CẬP NHẬT POLICY (PUT /api/policies/{id})
	// =====================================================================

	@Transactional
	public PolicyResponse updatePolicy(Integer id, PolicyUpdateRequest request) {
		Policy policy = policyRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorCode.POLICY_NOT_FOUND));

		// Validate code unique (trừ chính nó)
		if (policyRepository.existsByCodeAndIdNot(request.getCode(), id)) {
			throw new AppException(ErrorCode.POLICY_CODE_ALREADY_EXISTS);
		}

		// Validate date range
		validateDateRange(request.getEffectiveFrom(), request.getEffectiveTo());

		policy.setCode(request.getCode());
		policy.setTitle(request.getTitle());
		policy.setDescription(request.getDescription());
		policy.setEffectiveFrom(request.getEffectiveFrom());
		policy.setEffectiveTo(request.getEffectiveTo());

		policy = policyRepository.save(policy);

		log.info("Cập nhật policy thành công: id={}", id);
		return policyMapper.toPolicyResponse(policy);
	}

	// =====================================================================
	// 3. XÓA POLICY (DELETE /api/policies/{id})
	// =====================================================================

	@Transactional
	public void deletePolicy(Integer id) {
		Policy policy = policyRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorCode.POLICY_NOT_FOUND));

		policyRepository.delete(policy);

		log.info("Xóa policy thành công: id={}", id);
	}

	// =====================================================================
	// 4. LẤY CHI TIẾT POLICY (GET /api/policies/{id})
	// =====================================================================

	public PolicyResponse getPolicyDetail(Integer id) {
		Policy policy = policyRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorCode.POLICY_NOT_FOUND));

		return policyMapper.toPolicyResponse(policy);
	}

	// =====================================================================
	// 5. LẤY DANH SÁCH POLICY (GET /api/policies) - search, filter, sort, paging
	// =====================================================================

	public PolicyPageResponse getAllPolicies(
			String keyword,
			LocalDate effectiveFrom,
			LocalDate effectiveTo,
			Pageable pageable) {

		Page<Policy> result = policyRepository.findAll(
				PolicySpecification.filter(keyword, effectiveFrom, effectiveTo),
				pageable
		);

		return PolicyPageResponse.builder()
				.items(result.getContent()
						.stream()
						.map(policyMapper::toPolicyResponse)
						.toList())
				.page(result.getNumber())
				.size(result.getSize())
				.totalElements(result.getTotalElements())
				.totalPages(result.getTotalPages())
				.build();
	}

	// =====================================================================
	// PRIVATE HELPER METHODS
	// =====================================================================

	private void validateDateRange(LocalDate effectiveFrom, LocalDate effectiveTo) {
		if (effectiveFrom != null && effectiveTo != null) {
			if (!effectiveFrom.isBefore(effectiveTo)) {
				throw new AppException(ErrorCode.POLICY_DATE_INVALID);
			}
		}
	}

	private User getCurrentUser() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new AppException(ErrorCode.POLICY_MANAGER_NOT_FOUND));
	}
}
