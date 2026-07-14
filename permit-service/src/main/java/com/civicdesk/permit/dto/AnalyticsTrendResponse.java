package com.civicdesk.permit.dto;


import java.time.LocalDate;

public interface AnalyticsTrendResponse {

    LocalDate getDate();

    Long getCount();
}