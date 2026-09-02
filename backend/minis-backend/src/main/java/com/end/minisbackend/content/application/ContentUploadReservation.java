package com.end.minisbackend.content.application;

import com.end.minisbackend.content.domain.ContentItem;

record ContentUploadReservation(ContentItem content, boolean created) {}
