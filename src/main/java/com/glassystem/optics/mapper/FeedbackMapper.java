package com.glassystem.optics.mapper;

import com.glassystem.optics.dto.response.FeedbackResponse;
import com.glassystem.optics.entity.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "feedbackId", source = "id")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(buildCustomerName(feedback))")
    FeedbackResponse toFeedbackResponse(Feedback feedback);

    List<FeedbackResponse> toFeedbackResponseList(List<Feedback> feedbacks);

    default String buildCustomerName(Feedback feedback) {
        if (feedback.getCustomer() == null) return null;
        String firstName = feedback.getCustomer().getFirstName();
        String lastName = feedback.getCustomer().getLastName();
        if (firstName != null && lastName != null) return firstName + " " + lastName;
        if (firstName != null) return firstName;
        if (lastName != null) return lastName;
        return feedback.getCustomer().getUsername();
    }
}
