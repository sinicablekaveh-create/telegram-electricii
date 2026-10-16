FROM --platform=linux/amd64 ubuntu:24.04 AS build

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -yq default-jdk g++ git gperf make perl php-cli unzip wget && rm -rf /var/lib/apt/lists/*

ARG COMMIT_HASH
ARG ANDROID_NDK_VERSION
ARG OPENSSL_VERSION
ARG TDLIB_INTERFACE=JSONJava
ARG ANDROID_API_LEVEL=26
ARG ANDROID_ABIS="arm64-v8a armeabi-v7a x86_64 x86"

WORKDIR /home
RUN git clone https://github.com/tdlib/td.git td \
    && cd td \
    && git checkout "$COMMIT_HASH" \
    && git merge-base --is-ancestor 872d8ebd3ba9d922169839e6a24cee08b02b328a "$COMMIT_HASH"

WORKDIR /home/td/example/android
RUN ./fetch-sdk.sh SDK "$ANDROID_NDK_VERSION"
RUN ./build-openssl.sh SDK "$ANDROID_NDK_VERSION" openssl "$OPENSSL_VERSION"
RUN ./build-tdlib.sh SDK "$ANDROID_NDK_VERSION" openssl c++_static "$TDLIB_INTERFACE" "$ANDROID_API_LEVEL" "$ANDROID_ABIS" \
    && rm -rf build-native-* build-*

RUN mkdir -p /tdlib-output && cp tdlib/tdlib*.zip /tdlib-output/

FROM scratch
COPY --from=build /tdlib-output/* /
