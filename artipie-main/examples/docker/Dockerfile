FROM alpine:3.22.0

WORKDIR /test
RUN apk --update --no-cache add bash docker-cli
COPY ./run.sh /test/
CMD "/test/run.sh"
