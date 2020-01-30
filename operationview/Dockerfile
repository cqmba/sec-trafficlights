FROM node:alpine AS builder

WORKDIR /app

COPY . .

RUN npm install && \
    npm run build

FROM nginx:alpine

COPY --from=builder /app/dist/* /usr/share/nginx/html/
COPY docker_conf/ssl.conf /etc/nginx/conf.d/ssl.conf
COPY docker_conf/ca.cert /etc/nginx/certificates/ca.cert
COPY docker_conf/ca.key /etc/nginx/certificates/ca.key

RUN rm /etc/nginx/conf.d/default.conf

EXPOSE 80 443
