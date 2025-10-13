FROM alpine:3.22.2
WORKDIR /test
RUN apk --update --no-cache add curl bash
COPY run.sh run.sh
CMD /test/run.sh
