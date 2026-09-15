package com.logistics.service;

import com.logistics.dto.response.AiRecommendationResponse;
import com.logistics.dto.request.OperationalContextRequest;

/**
 * The single seam between Spring Boot and whatever AI technology is
 * eventually chosen (Spring AI in-JVM, or an external Python/FastAPI/
 * LangGraph service over HTTP - undecided, per project Rule 8). Nothing
 * outside this interface should know or care which one it is.
 *
 * CRITICAL: implementations of this interface must be pure recommendation
 * generators. They must never call DispatchService.approve()/execute(),
 * never receive a DataSource, and never be trusted without the caller
 * routing the response through ComplianceService.evaluate() again.
 */
public interface AiRecommendationClient {
    AiRecommendationResponse getRecommendation(OperationalContextRequest context);
}