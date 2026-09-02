package com.end.minisbackend.user.domain.enums;

/** MINIS role stored in PostgreSQL and never trusted from Firebase claims. */
public enum UserRole {
  VIEWER,
  CREATOR,
  ADMIN
}
