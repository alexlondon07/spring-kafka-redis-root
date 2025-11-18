package com.alexlondon07.news_api.models.dto.response.enums;

import com.alexlondon07.news_api.models.dto.response.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String code;
    private ErrorType errorType;
    private String message;
    private List<String> details;
    private String timestamp;

}