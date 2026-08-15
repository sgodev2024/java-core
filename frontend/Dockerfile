FROM node:22-alpine

WORKDIR /app
COPY dist/standalone ./
COPY dist/client ./dist/client

ENV NODE_ENV=production
ENV PORT=3000
EXPOSE 3000

CMD ["node", "server.js"]
