FROM node:24-alpine AS event-build
WORKDIR /workspace/web/event-app

COPY web/event-app/package.json web/event-app/package-lock.json ./
RUN npm ci

COPY web/event-app ./

ARG VITE_FIREBASE_API_KEY
ARG VITE_FIREBASE_AUTH_DOMAIN
ARG VITE_FIREBASE_PROJECT_ID
ARG VITE_FIREBASE_APP_ID
ARG VITE_FIREBASE_STORAGE_BUCKET

ENV VITE_FIREBASE_API_KEY=${VITE_FIREBASE_API_KEY}
ENV VITE_FIREBASE_AUTH_DOMAIN=${VITE_FIREBASE_AUTH_DOMAIN}
ENV VITE_FIREBASE_PROJECT_ID=${VITE_FIREBASE_PROJECT_ID}
ENV VITE_FIREBASE_APP_ID=${VITE_FIREBASE_APP_ID}
ENV VITE_FIREBASE_STORAGE_BUCKET=${VITE_FIREBASE_STORAGE_BUCKET}
ENV VITE_API_BASE_URL=

RUN npm run build

FROM nginx:1.27-alpine
COPY infrastructure/nginx/demo.conf /etc/nginx/conf.d/default.conf
COPY --from=event-build /workspace/web/event-app/dist /usr/share/nginx/html
