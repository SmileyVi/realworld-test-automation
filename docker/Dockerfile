# syntax=docker/dockerfile:1
FROM node:24-bookworm-slim AS build
WORKDIR /opt/conduit
ADD --checksum=sha256:cd95760904e587b12d04d44425f72664f94bc6b87b15682737d01a6a543952f6 \
    https://codeload.github.com/TonyMckes/conduit-realworld-example-app/tar.gz/5e127d8569b300e0a21dc2c20ea680da4967b1aa /tmp/conduit.tar.gz
RUN tar -xzf /tmp/conduit.tar.gz --strip-components=1 \
    && npm ci --no-audit --no-fund \
    && npm run build -w frontend \
    && npm prune --omit=dev --no-audit --no-fund

FROM node:24-bookworm-slim
WORKDIR /opt/conduit
COPY --from=build /opt/conduit/node_modules ./node_modules
COPY --from=build /opt/conduit/backend ./backend
COPY --from=build /opt/conduit/frontend/dist ./frontend/dist
COPY --from=build /opt/conduit/LICENSE ./LICENSE
WORKDIR /opt/conduit/backend
USER node
EXPOSE 3001
CMD ["node", "index.js"]
