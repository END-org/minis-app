package com.end.minisbackend.content.domain.policy;

/** Length limits shared by content transport validation and domain invariants. */
public final class ContentTextPolicy {
  public static final int TITLE_MAX = 140;
  public static final int DESCRIPTION_MAX = 1000;

  private ContentTextPolicy() {}
}
