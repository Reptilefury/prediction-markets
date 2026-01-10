package com.oregonmarkets.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatsResponse {
    private long total;
    private long active;
    private long inactive;
    private long suspended;
    private long twoFactorEnabled;
}
