package com.jaeuk.job_ai.dto.DocumentDto;

import com.jaeuk.job_ai.entity.Document;
import com.jaeuk.job_ai.entity.DocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailResponse {

    private DocumentResponse document;
    private List<DocumentChunkResponse> chunks;

    public static DocumentDetailResponse of(Document document, List<DocumentChunk> chunks) {
        return DocumentDetailResponse.builder()
                .document(DocumentResponse.from(document))
                .chunks(chunks.stream().map(DocumentChunkResponse::from).toList())
                .build();
    }
}
