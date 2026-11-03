# Stage 1: build
FROM node:20-alpine AS build
WORKDIR /app
RUN npm install -g pnpm@9
COPY package.json ./
RUN pnpm install --ignore-scripts
COPY . .
RUN pnpm build

# Stage 2: serve
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s \
    CMD wget -qO- http://localhost:80/ || exit 1
