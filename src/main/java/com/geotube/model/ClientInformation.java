package com.geotube.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientInformation {
    private String userName;
    private String email;
    private String clientId;
    private String token;
}
