package com.end.minisbackend.content.domain;

import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum ContentCategory {
  EDUCATION("Education"),
  TECHNOLOGY("Technology"),
  BUSINESS("Business"),
  HEALTH("Health"),
  SCIENCE("Science"),
  HISTORY("History"),
  SKILLS("Skills"),
  MINI_DOCUMENTARY("Mini documentaries"),
  STORYTELLING("Storytelling"),
  CAREER_ADVICE("Career advice"),
  LIFE_TIPS("Life tips"),
  CINEMATIC_STORIES("Cinematic stories"),
  CREATIVE_FILMMAKING("Creative filmmaking");

  private final String displayName;

  ContentCategory(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }

  public static ContentCategory parse(String value) {
    if (value == null || value.isBlank()) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    String normalized = value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(category -> category.name().equals(normalized))
        .findFirst()
        .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_ERROR));
  }

  public static List<ContentCategory> catalog() {
    return List.of(values());
  }
}
