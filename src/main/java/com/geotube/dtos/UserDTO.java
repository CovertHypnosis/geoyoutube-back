package com.geotube.dtos;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String email;
    private String country;
    private Long updateTime;
}
