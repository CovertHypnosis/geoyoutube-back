package com.geotube.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendingInfoDTO {
    String country;
    Long updateTime;
    String videoLink;
    String comment;
}
