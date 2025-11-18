package com.alexlondon07.news_api.models.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DataResponse<T> {
    private String message;
    private Boolean status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
}