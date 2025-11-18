package com.alexlondon07.news_api.service.impl;

import com.alexlondon07.news_api.repository.NewsRepository;
import com.alexlondon07.news_api.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.alexlondon07.news_api.utils.Constants.TOPIC_NAME;

@Service
@RequiredArgsConstructor
public class NewsServiceImp implements NewsService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NewsRepository newsRepository;

    @Override
    public Mono<Void> publishToMessageBroker(String date) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC_NAME, null, date);
        return Mono.fromFuture(kafkaTemplate.send(producerRecord))
                .then();
    }

    @Override
    public Mono<Object> getNewsFromRedis(String date) {
        return newsRepository.getNews(date)
                .flatMap(Mono::just)
                .switchIfEmpty(Mono.defer(() -> publishToMessageBroker(date).then(Mono.empty())));
    }
}
