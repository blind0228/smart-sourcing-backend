// com.smart.backend.dto.RankingItem.java

package com.smart.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingItem {
    private int rank;
    private String keyword;
    private long searchRatio;
}