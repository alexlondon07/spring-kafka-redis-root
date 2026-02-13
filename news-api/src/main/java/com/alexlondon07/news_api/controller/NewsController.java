package com.alexlondon07.news_api.controller;


import com.alexlondon07.news_api.models.dto.response.DataResponse;
import com.alexlondon07.news_api.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "News API", description = "API for retrieving news data from MediaStack. Uses cache-aside pattern with Kafka for async data fetching.")
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "Get news by date", description = "Retrieves news articles for a specific date. If data is not cached, it publishes a request to Kafka for async processing.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "News data found and returned successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DataResponse.class))),
            @ApiResponse(responseCode = "404", description = "News data not found in cache",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DataResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date format. Must be YYYY-MM-DD")
    })
    @GetMapping
    public Mono<ResponseEntity<DataResponse<Object>>> getNews(
            @Parameter(description = "Date in YYYY-MM-DD format", required = true, example = "2024-01-15")
            @NotBlank(message = DATE_NOT_BLANK_MESSAGE)
            @Pattern(regexp = DATE_FORMAT, message = DATE_PATTERN_MESSAGE)
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
