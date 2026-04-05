package io.quintkard.quintkardapp.card;

import io.quintkard.quintkardapp.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "card_embeddings")
public class CardEmbedding extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_fk", nullable = false)
    private Card card;

    @Column(nullable = false)
    private String embeddingModel;

    @Column(nullable = false)
    private String chunkingStrategy;

    @Column(nullable = false)
    private int chunkIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TextChunkType chunkType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @ColumnTransformer(write = "?::vector")
    @Column(nullable = false, columnDefinition = "vector(3072)")
    private String embeddingVector;

    protected CardEmbedding() {
    }

    public CardEmbedding(
            Card card,
            String embeddingModel,
            String chunkingStrategy,
            int chunkIndex,
            TextChunkType chunkType,
            String chunkText,
            float[] embeddingVector
    ) {
        this.card = card;
        this.embeddingModel = embeddingModel;
        this.chunkingStrategy = chunkingStrategy;
        this.chunkIndex = chunkIndex;
        this.chunkType = chunkType;
        this.chunkText = chunkText;
        this.embeddingVector = toVectorLiteral(embeddingVector);
    }

    public UUID getId() {
        return id;
    }

    public Card getCard() {
        return card;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public String getChunkingStrategy() {
        return chunkingStrategy;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public TextChunkType getChunkType() {
        return chunkType;
    }

    public String getChunkText() {
        return chunkText;
    }

    public float[] getEmbeddingVector() {
        String value = embeddingVector;
        if (value == null || value.length() < 2) {
            return new float[0];
        }

        String[] tokens = value.substring(1, value.length() - 1).split(",");
        float[] vector = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            vector[i] = Float.parseFloat(tokens[i]);
        }
        return vector;
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}
