FROM ubuntu:latest
LABEL authors="sspur"

ENTRYPOINT ["top", "-b"]