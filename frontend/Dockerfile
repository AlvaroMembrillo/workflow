# syntax=docker/dockerfile:1

# ---- Compilación de la aplicación Angular ----
FROM node:24-alpine AS compilacion
WORKDIR /codigo

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
RUN npm run build

# ---- Servidor: nginx con los ficheros estáticos y el proxy a la API ----
FROM nginx:stable-alpine
COPY nginx/ /etc/nginx/conf.d/
COPY --from=compilacion /codigo/dist/workflow/browser/ /usr/share/nginx/html/
EXPOSE 80
