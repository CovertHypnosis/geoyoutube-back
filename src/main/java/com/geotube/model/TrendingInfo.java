package com.geotube.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendingInfo {
    String country;
    Long updateTime;
    String videoLink;
    String comment;
}
