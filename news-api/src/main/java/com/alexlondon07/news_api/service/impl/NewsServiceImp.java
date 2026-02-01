package com.alexlondon07.news_api.service.impl;

import com.alexlondon07.news_api.config.CustomDatadogConfig;
import com.alexlondon07.news_api.repository.NewsRepository;
import com.alexlondon07.news_api.service.NewsService;
import datadog.trace.api.DDTags;
import datadog.trace.api.Trace;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.alexlondon07.news_api.utils.Constants.TOPIC_NAME;

@Service
public class NewsServiceImp implements NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsServiceImp.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NewsRepository newsRepository;
    private final CustomDatadogConfig.RedisMetricsCollector redisMetricsCollector;
    private final MeterRegistry meterRegistry;

    public NewsServiceImp(KafkaTemplate<String, String> kafkaTemplate,
                         NewsRepository newsRepository,
                         CustomDatadogConfig.RedisMetricsCollector redisMetricsCollector,
                         MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.newsRepository = newsRepository;
        this.redisMetricsCollector = redisMetricsCollector;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Trace(operationName = "kafka.publish")
    public Mono<Void> publishToMessageBroker(String date) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC_NAME, null, date);
        
        return Mono.fromFuture(kafkaTemplate.send(producerRecord))
                .doOnSuccess(result -> {
                    meterRegistry.counter("kafka.producer.success", 
                            "topic", TOPIC_NAME,
                            "service", "news-api").increment();
                    log.info("Successfully published date {} to Kafka topic {}", date, TOPIC_NAME);
                })
                .doOnError(error -> {
                    meterRegistry.counter("kafka.producer.error", 
                            "topic", TOPIC_NAME,
                            "service", "news-api").increment();
                    log.error("Failed to publish date {} to Kafka topic {}: {}", 
                            date, TOPIC_NAME, error.getMessage());
                })
                .then()
                .doFinally(signalType -> sample.stop(Timer.builder("kafka.producer.duration")
                        .description("Time taken to publish message to Kafka")
                        .tag("topic", TOPIC_NAME)
                        .tag("service", "news-api")
                        .register(meterRegistry)));
    }

    @Override
    @Trace(operationName = "news.get")
    public Mono<Object> getNewsFromRedis(String date) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return newsRepository.getNews(date)
                .flatMap(Mono::just)
                .doOnSuccess(data -> {
                    meterRegistry.counter("news.request.success", 
                            "service", "news-api",
                            "source", "cache").increment();
                    log.info("Cache hit for date {}", date);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    meterRegistry.counter("news.request.miss", 
                            "service", "news-api",
                            "source", "cache").increment();
                    log.info("Cache miss for date {}, publishing to Kafka", date);
                    return publishToMessageBroker(date).then(Mono.empty());
                }))
                .doOnError(error -> {
                    meterRegistry.counter("news.request.error", 
                            "service", "news-api").increment();
                    log.error("Error getting news for date {}: {}", date, error.getMessage());
                })
                .doFinally(signalType -> sample.stop(Timer.builder("news.get.duration")
                        .description("Time taken to get news")
                        .tag("service", "news-api")
                        .register(meterRegistry)));
    }
}