import type { SyntheticEvent } from "react";
import type { AdminCreatorRoleFormValue } from "./creatorRoleFormState";

interface AdminCreatorRoleFormProps {
  readonly pending: boolean;
  readonly value: AdminCreatorRoleFormValue;
  readonly onChange: (field: keyof AdminCreatorRoleFormValue, value: string) => void;
  readonly onSubmit: (event: SyntheticEvent<HTMLFormElement>) => void;
}

export function AdminCreatorRoleForm({
  onChange,
  onSubmit,
  pending,
  value,
}: AdminCreatorRoleFormProps) {
  return (
    <form className="profile-form stack" onSubmit={onSubmit}>
      <h2>Creator role</h2>
      <label>
        Public slug
        <input
          value={value.slug}
          maxLength={120}
          onChange={(event) => {
            onChange("slug", event.currentTarget.value);
          }}
          required
        />
      </label>
      <label>
        Public display name
        <input
          value={value.displayName}
          maxLength={120}
          onChange={(event) => {
            onChange("displayName", event.currentTarget.value);
          }}
          required
        />
      </label>
      <label>
        Public bio
        <textarea
          value={value.bio}
          maxLength={500}
          onChange={(event) => {
            onChange("bio", event.currentTarget.value);
          }}
        />
      </label>
      <label>
        Avatar URL
        <input
          value={value.avatarUrl}
          maxLength={1024}
          onChange={(event) => {
            onChange("avatarUrl", event.currentTarget.value);
          }}
        />
      </label>
      <label>
        Audit reason
        <input
          value={value.reason}
          maxLength={300}
          onChange={(event) => {
            onChange("reason", event.currentTarget.value);
          }}
        />
      </label>
      <button type="submit" disabled={pending}>
        Grant creator
      </button>
    </form>
  );
}
