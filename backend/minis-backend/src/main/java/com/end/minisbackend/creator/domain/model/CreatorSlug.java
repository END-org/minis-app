package com.end.minisbackend.creator.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class CreatorSlug {
  public static final int MAX_LENGTH = 80;

  private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
  private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
  private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+|-+$)");

  private CreatorSlug() {}

  public static String normalize(String input) {
    if (!StringUtils.hasText(input)) {
      throw new IllegalArgumentException("creator slug is required");
    }
    String ascii =
        COMBINING_MARKS
            .matcher(Normalizer.normalize(input.trim(), Normalizer.Form.NFKD))
            .replaceAll("");
    String slug =
        EDGE_HYPHENS
            .matcher(NON_SLUG_CHARS.matcher(ascii.toLowerCase(Locale.ROOT)).replaceAll("-"))
            .replaceAll("");
    if (!StringUtils.hasText(slug) || slug.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("creator slug is invalid");
    }
    return slug;
  }
}
