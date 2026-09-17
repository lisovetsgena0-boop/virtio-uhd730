FROM barichello/godot-ci:3.5.2 AS build

WORKDIR /game
COPY echo-rivne/chunk00.b64 /tmp/chunk00.b64
COPY echo-rivne/chunk01.b64 /tmp/chunk01.b64
COPY echo-rivne/chunk02.b64 /tmp/chunk02.b64
COPY echo-rivne/chunk03.b64 /tmp/chunk03.b64
COPY echo-rivne/chunk04.b64 /tmp/chunk04.b64
RUN cat /tmp/chunk00.b64 /tmp/chunk01.b64 /tmp/chunk02.b64 /tmp/chunk03.b64 /tmp/chunk04.b64 > /tmp/echo_rivne_game.b64 \
    && base64 -d /tmp/echo_rivne_game.b64 > /tmp/echo_rivne_game.zip \
    && unzip -q /tmp/echo_rivne_game.zip -d /game \
    && mkdir -p /game/build \
    && godot --version \
    && godot --verbose --path /game --export-debug "Android" /game/build/ECHO_RIVNE.apk \
    && test -s /game/build/ECHO_RIVNE.apk

FROM python:3.11-alpine
WORKDIR /app
COPY --from=build /game/build/ECHO_RIVNE.apk /app/ECHO_RIVNE.apk
RUN printf '%s\n' '<!doctype html><meta charset="utf-8"><title>ECHO//RIVNE</title><style>body{font-family:sans-serif;background:#081018;color:#e8ffff;text-align:center;padding:10vh}a{font-size:24px;color:#41e5d1}</style><h1>ECHO//RIVNE</h1><p>Android 5.0+ debug build</p><p><a href="/ECHO_RIVNE.apk">Download ECHO_RIVNE.apk</a></p>' > /app/index.html
EXPOSE 8080
CMD ["python","-m","http.server","8080","--bind","0.0.0.0"]
