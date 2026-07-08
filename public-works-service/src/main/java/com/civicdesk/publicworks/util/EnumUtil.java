package com.civicdesk.publicworks.util;

import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.exception.BadRequestException;

import java.util.Arrays;

public class EnumUtil {

    public static WorkCategory parseWorkCategory(
            String value) {

        return Arrays.stream(
                        WorkCategory.values())
                .filter(category ->
                        category.name()
                                .equalsIgnoreCase(
                                        value.replace(
                                                " ",
                                                "_")))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Invalid work category"));
    }
}