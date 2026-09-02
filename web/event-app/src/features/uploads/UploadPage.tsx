import { Link } from "react-router-dom";
import { useMemo, useState, type SyntheticEvent } from "react";
import { toApiFailureView, type ApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { useCategoriesQuery } from "../feed/feedQueries";
import { MAX_UPLOAD_BYTES, type ContentUploadResponse } from "./uploadApi";
import { useUploadContentMutation } from "./uploadQueries";

export function UploadPage() {
  const categories = useCategoriesQuery();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(createIdempotencyKey);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState<ContentUploadResponse | null>(null);
  const [failure, setFailure] = useState<ApiFailureView | null>(null);
  const upload = useUploadContentMutation((event) => {
    setProgress(event.percent);
  });

  const fileProblem = useMemo(() => validateFile(file), [file]);
  const selectedCategory =
    category.length > 0 ? category : (categories.data?.[0]?.value ?? "");
  const canSubmit =
    Boolean(file) &&
    !fileProblem &&
    title.trim().length > 0 &&
    description.trim().length > 0 &&
    selectedCategory.length > 0 &&
    !upload.isPending;

  function handleFile(nextFile: File | null) {
    setFile(nextFile);
    setFailure(null);
    setResult(null);
    setProgress(0);
    setIdempotencyKey(createIdempotencyKey());
  }

  function resetForm() {
    setTitle("");
    setDescription("");
    setFile(null);
    setFailure(null);
    setResult(null);
    setProgress(0);
    setIdempotencyKey(createIdempotencyKey());
  }

  function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file || !canSubmit) {
      return;
    }
    setFailure(null);
    setResult(null);
    upload.mutate(
      {
        file,
        idempotencyKey,
        metadata: {
          category: selectedCategory,
          description: description.trim(),
          title: title.trim(),
        },
      },
      {
        onError: (error) => {
          const nextFailure = toApiFailureView(error);
          setFailure(nextFailure);
          setProgress(0);
          if (requiresNewAttemptKey(nextFailure.code)) {
            setIdempotencyKey(createIdempotencyKey());
          }
        },
        onSuccess: (response) => {
          setProgress(100);
          setResult(response);
        },
      },
    );
  }

  if (categories.isLoading) {
    return <LoadingState message="Loading upload settings" />;
  }

  if (categories.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(categories.error)}
        onRetry={() => {
          void categories.refetch();
        }}
      />
    );
  }

  return (
    <section className="upload-view" aria-labelledby="upload-title">
      <div className="page-heading">
        <p className="section-kicker">Creator studio</p>
        <h1 id="upload-title">Upload Event Video</h1>
      </div>

      <form className="upload-form" onSubmit={handleSubmit}>
        <label className="upload-dropzone">
          <input
            type="file"
            accept="video/mp4,.mp4"
            aria-label="Choose MP4 video"
            disabled={upload.isPending}
            onChange={(event) => {
              handleFile(event.currentTarget.files?.[0] ?? null);
            }}
          />
          <span>{file ? file.name : "Choose one MP4 video"}</span>
          <small>{file ? formatBytes(file.size) : "Vertical H.264 MP4, up to 200 MB."}</small>
        </label>
        {fileProblem ? (
          <p className="form-error" role="alert">
            {fileProblem}
          </p>
        ) : null}

        <label>
          <span>Title</span>
          <input
            value={title}
            maxLength={140}
            disabled={upload.isPending}
            onChange={(event) => {
              setTitle(event.currentTarget.value);
            }}
          />
        </label>
        <label>
          <span>Description</span>
          <textarea
            value={description}
            maxLength={1000}
            rows={5}
            disabled={upload.isPending}
            onChange={(event) => {
              setDescription(event.currentTarget.value);
            }}
          />
        </label>
        <label>
          <span>Category</span>
          <select
            value={selectedCategory}
            disabled={upload.isPending}
            onChange={(event) => {
              setCategory(event.currentTarget.value);
            }}
          >
            {categories.data?.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>

        {upload.isPending ? (
          <div className="upload-progress" role="status" aria-live="polite">
            <span>Uploading</span>
            <progress max="100" value={progress} />
            <strong>{progress}%</strong>
          </div>
        ) : null}

        <div className="form-actions">
          <button type="submit" disabled={!canSubmit}>
            {upload.isPending ? "Uploading" : "Publish video"}
          </button>
          <button type="button" disabled={upload.isPending} onClick={resetForm}>
            Clear
          </button>
        </div>
      </form>

      {failure ? <ApiFailureNotice failure={failure} /> : null}
      {result ? (
        <section className="upload-result" aria-live="polite">
          <h2>{result.title}</h2>
          <p className="status-pill">{result.status}</p>
          {result.content ? (
            <Link to={`/content/${result.content.id}`}>Open published video</Link>
          ) : null}
          <button type="button" onClick={resetForm}>
            Upload another
          </button>
        </section>
      ) : null}
    </section>
  );
}

function validateFile(file: File | null): string | null {
  if (!file) {
    return null;
  }
  if (file.size <= 0) {
    return "Choose a non-empty MP4 file.";
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return "Choose an MP4 file that is 200 MB or smaller.";
  }
  if (!file.name.toLowerCase().endsWith(".mp4")) {
    return "Use a file name ending in .mp4.";
  }
  if (file.type && file.type.toLowerCase() !== "video/mp4") {
    return "The browser must identify the selected file as video/mp4.";
  }
  return null;
}

function requiresNewAttemptKey(code: string): boolean {
  return (
    code === "UPLOAD_ALREADY_FAILED" ||
    code === "UPLOAD_TOO_LARGE" ||
    code === "UPLOAD_VALIDATION_FAILED" ||
    code === "VALIDATION_ERROR"
  );
}

function createIdempotencyKey(): string {
  return `upload-${globalThis.crypto.randomUUID()}`;
}

function formatBytes(bytes: number): string {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 1,
    style: "unit",
    unit: "megabyte",
    unitDisplay: "short",
  }).format(bytes / (1024 * 1024));
}
