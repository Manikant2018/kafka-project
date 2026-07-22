package com.api.logging.dto;

import lombok.Data;

@Data
public class TestRequest {
    private String name;
    private String email;
    private String password; // This should be masked
}
