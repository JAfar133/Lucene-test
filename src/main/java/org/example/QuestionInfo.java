package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Data
public class QuestionInfo {
    private String prompt;
    private String request;
    private String response;
    private long duration;
    private String data;
}
