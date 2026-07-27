package com.end.minisbackend.admin.application;

import java.util.List;

public record AdminUserPage(List<AdminUserView> users, int page, int size, boolean hasNext) {
  public AdminUserPage {
    users = List.copyOf(users);
  }

  @Override
  public List<AdminUserView> users() {
    return List.copyOf(users);
  }
}
