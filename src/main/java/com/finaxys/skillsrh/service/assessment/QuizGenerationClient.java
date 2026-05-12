package com.finaxys.skillsrh.service.assessment;

public interface QuizGenerationClient {

    QuizModels.GeneratedQuizBlock generateBlock(QuizModels.QuizGenerationRequest request);
}
