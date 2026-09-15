package com.logistics.service.impl;

import com.logistics.dto.response.AiRecommendationResponse;
import com.logistics.dto.request.OperationalContextRequest;
import com.logistics.dto.response.AiRecommendationResponse;
import com.logistics.service.AiRecommendationClient;
import org.springframework.stereotype.Service;

/**
 * TEMPORARY. No real AI technology has been selected yet (Rule 8). This
 * implementation exists only so AiRecommendationClient is a valid, injectable
 * Spring bean and the rest of the system (once a caller exists) compiles and
 * can be tested end-to-end with a deterministic, non-LLM stand-in.
 * MUST be replaced, not extended, once Spring AI or an external service is chosen.
 */
@Service
public class NoOpAiRecommendationClient implements AiRecommendationClient {
    @Override
    public AiRecommendationResponse getRecommendation(OperationalContextRequest context) {
        throw new UnsupportedOperationException(
                "No AI recommendation technology has been selected yet - see project Rule 8. " +
                        "This is a placeholder bean, not a functional implementation.");
    }
}