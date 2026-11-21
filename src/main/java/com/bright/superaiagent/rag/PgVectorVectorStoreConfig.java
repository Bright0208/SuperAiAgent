package com.bright.superaiagent.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.stream.Gatherers;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class PgVectorVectorStoreConfig {

    private static final int DASHSCOPE_MAX_BATCH_SIZE = 10;

    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        TokenCountBatchingStrategy tokenBatchingStrategy = new TokenCountBatchingStrategy();
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .batchingStrategy(documents -> tokenBatchingStrategy.batch(documents).stream()
                        .flatMap(batch -> batch.stream().gather(Gatherers.windowFixed(DASHSCOPE_MAX_BATCH_SIZE)))
                        .toList())
                .dimensions(1024)
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("vector_store_1024")
                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
                .build();
    }

    @Bean
    InitializingBean pgVectorDocumentInitializer(@Qualifier("pgVectorVectorStore") VectorStore vectorStore,
                                                 LoveAppDocumentLoader documentLoader) {
        return () -> vectorStore.add(documentLoader.loadMarkdowns());
    }
}
