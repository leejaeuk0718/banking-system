package com.jaeuk.job_ai.dto.DocumentDto;

import com.jaeuk.job_ai.entity.DocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkResponse {

    private Long id;
    private int chunkIndex;
    private String contentPreview;
    private String vectorId;

    public static DocumentChunkResponse from(DocumentChunk chunk) {
        return DocumentChunkResponse.builder()
                .id(chunk.getId())
                .chunkIndex(chunk.getChunkIndex())
                .contentPreview(chunk.getContentPreview())
                .vectorId(chunk.getVectorId())
                .build();
    }
}
