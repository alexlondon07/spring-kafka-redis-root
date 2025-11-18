package com.alexlondon07.news_api.controller;


import com.alexlondon07.news_api.models.dto.response.DataResponse;
import com.alexlondon07.news_api.service.NewsService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static com.alexlondon07.news_api.utils.Constants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping( value = "/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public Mono<ResponseEntity<DataResponse<Object>>> getNews(
            @NotBlank(message = DATE_NOT_BLANK_MESSAGE)
            @Pattern(regexp = DATE_FORMAT)
            @RequestParam( name = "date") String date) {

        return newsService.getNewsFromRedis(date)
                .flatMap( data -> Mono.just(
                        ResponseEntity.status(HttpStatus.OK)
                                .body(DataResponse.builder()
                                        .message(DATA_FOUND_MESSAGE)
                                        .status(Boolean.TRUE)
                                        .data(data)
                                        .build())))
                .switchIfEmpty(Mono.just(
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(DataResponse.builder()
                                        .message(DATA_NOT_FOUND_MESSAGE)
                                        .status(Boolean.FALSE)
                                        .build())));
    }
}
