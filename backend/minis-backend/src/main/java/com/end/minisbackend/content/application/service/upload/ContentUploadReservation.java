package com.end.minisbackend.content.application.service.upload;

import com.end.minisbackend.content.domain.model.ContentItem;

record ContentUploadReservation(ContentItem content, boolean created) {}
