package com.geotube.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
public class UserInformation extends BaseEntity{
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private User user;
    private Long lastRequestTime;
    private String country;
    private String youtubeLink;
    private String youtubeComment;
}
