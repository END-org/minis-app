package com.end.minisbackend.content.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.content.domain.model.media.MediaInspection;
import com.end.minisbackend.content.domain.model.media.MediaValidationResult;
import com.end.minisbackend.shared.error.ApiException;
import org.junit.jupiter.api.Test;

class EventMediaProfilePolicyTest {
  private final EventMediaProfilePolicy policy = new EventMediaProfilePolicy();

  @Test
  void acceptsSingleEventMp4Profile() {
    MediaValidationResult result =
        policy.validate(
            new MediaInspection(
                "mov,mp4,m4a,3gp,3g2,mj2", "h264", "aac", 60_000, 1080, 1920, 30d, 2),
            10_000_000L);

    assertThat(result.contentType()).isEqualTo("video/mp4");
    assertThat(result.durationMs()).isEqualTo(60_000L);
  }

  @Test
  void rejectsUnsupportedContainerCodecAudioDurationSizeResolutionAndFrameRate() {
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("matroska", "h264", "aac", 60_000, 1080, 1920, 30d, 2),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("mov,mp4", "hevc", "aac", 60_000, 1080, 1920, 30d, 2),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("mov,mp4", "h264", "mp3", 60_000, 1080, 1920, 30d, 2),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("mov,mp4", "h264", "aac", 91_000, 1080, 1920, 30d, 2),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("mov,mp4", "h264", "aac", 60_000, 1080, 1920, 30d, 2),
                    EventMediaProfilePolicy.MAX_SIZE_BYTES + 1))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("mov,mp4", "h264", "aac", 60_000, 1920, 1080, 30d, 2),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("mov,mp4", "h264", "aac", 60_000, 1080, 1920, 60d, 2),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void rejectsMalformedFrameRateAndUnexpectedStreams() {
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("mov,mp4", "h264", "aac", 60_000, 1080, 1920, 0d, 2),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection(
                        "mov,mp4", "h264", "aac", 60_000, 1080, 1920, Double.NaN, 2),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                policy.validate(
                    new MediaInspection("mov,mp4", "h264", "aac", 60_000, 1080, 1920, 30d, 3),
                    10_000_000L))
        .isInstanceOf(ApiException.class);
  }
}
