package com.smart.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MarketAnalysisRequest {

    // Python이 "search_keyword"라고 보내면 자바의 searchKeyword에 넣음
    @JsonProperty("search_keyword")
    private String searchKeyword;

    @JsonProperty("keyword")
    private String category;

    @JsonProperty("average_price")
    private int averagePrice;

    @JsonProperty("lowest_price")
    private int lowestPrice;

    @JsonProperty("sample_count")
    private int sampleCount;

    @JsonProperty("top_item_name")
    private String topItemName;
}