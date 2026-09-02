import type { AdminUserResponse } from "./adminApi";

export interface AdminCreatorRoleFormValue {
  readonly slug: string;
  readonly displayName: string;
  readonly bio: string;
  readonly avatarUrl: string;
  readonly reason: string;
}

export function creatorRoleFormFromUser(
  user: AdminUserResponse,
): AdminCreatorRoleFormValue {
  return {
    avatarUrl: user.creatorProfile?.avatarUrl ?? "",
    bio: user.creatorProfile?.bio ?? "",
    displayName: user.creatorProfile?.displayName ?? user.displayName ?? user.email ?? "",
    reason: "",
    slug: user.creatorProfile?.slug ?? "",
  };
}
