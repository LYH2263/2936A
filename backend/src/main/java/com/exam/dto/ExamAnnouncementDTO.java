package com.exam.dto;

import com.exam.config.PlainTextDeserializer;
import com.exam.config.RichTextDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
public class ExamAnnouncementDTO {
    @JsonDeserialize(using = PlainTextDeserializer.class)
    private String title;

    @JsonDeserialize(using = RichTextDeserializer.class)
    private String content;

    private Boolean isPinned = false;
}
