
package com.bright.superaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

@Slf4j
public final class MyLoggerAdvisor
        implements CallAdvisor, StreamAdvisor {

    private final int order;

    private MyLoggerAdvisor(int order) {
        this.order = order;
    }

    @Override
    public String getName() {
        return "MyLoggerAdvisor";
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    // 普通调用：chatClient.prompt().call()
    @Override
    public ChatClientResponse adviseCall(
            ChatClientRequest request,
            CallAdvisorChain chain) {

        long startTime = System.currentTimeMillis();

        logRequest(request);

        try {
            ChatClientResponse response =
                    chain.nextCall(request);

            logResponse(response);

            log.info("AI调用耗时: {} ms",
                    System.currentTimeMillis() - startTime);
//            log.info("请求内容: {}",request.prompt().getUserMessage());

            return response;

        } catch (Exception e) {
            log.error("AI调用失败，耗时: {} ms",
                    System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    // 流式调用：chatClient.prompt().stream()
    @Override
    public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest request,
            StreamAdvisorChain chain) {

        return Flux.defer(() -> {

            long startTime = System.currentTimeMillis();

            logRequest(request);

            Flux<ChatClientResponse> responseFlux =
                    chain.nextStream(request);

            // 聚合流式响应，仅用于记录完整回答
            return new ChatClientMessageAggregator()
                    .aggregateChatClientResponse(
                            responseFlux,
                            response -> {
                                logResponse(response);
                                log.info("AI流式调用耗时: {} ms",
                                        System.currentTimeMillis()
                                                - startTime);
                            }
                    )
                    .doOnError(e ->
                            log.error("AI流式调用失败", e)
                    );
        });
    }

    private void logRequest(ChatClientRequest request) {
//        if (log.isDebugEnabled()) {
            log.info("========== AI请求 ==========");
            log.info("请求内容: {}",
                    request.prompt().getUserMessage());
//        }
    }

    private void logResponse(ChatClientResponse response) {
//        if (log.isDebugEnabled()) {
            log.info("========== AI响应 ==========");
            log.info("响应内容: {}",
                    response.chatResponse());
//        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int order = 100;

        private Builder() {
        }

        public Builder order(int order) {
            Assert.isTrue(order >= 0,
                    "order must be >= 0");
            this.order = order;
            return this;
        }

        public MyLoggerAdvisor build() {
            return new MyLoggerAdvisor(this.order);
        }
    }
}
