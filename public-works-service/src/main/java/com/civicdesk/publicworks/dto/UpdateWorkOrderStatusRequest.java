package com.civicdesk.publicworks.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkOrderStatusRequest {

    private String status;

    private String remarks;
}