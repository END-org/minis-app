import type { ApiFailureView } from "../../core/errors/apiError";

export interface ForbiddenRouteState {
  readonly failure: ApiFailureView;
}
